package com.sowrov.germanarticlebangla.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sowrov.germanarticlebangla.BuildConfig
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "WortArtAuth"
private const val USERS_COLLECTION = "users"
private const val MAX_DELAY_MS = Int.MAX_VALUE.toLong()

data class AppUser(val uid: String, val email: String, val name: String)

data class AuthState(
    /** false when app/google-services.json was not added to the project. */
    val configured: Boolean = false,
    val loading: Boolean = false,
    val user: AppUser? = null,
    val isAdmin: Boolean = false,
    val premiumUntilMs: Long? = null,
    /** premiumUntil is in the future. */
    val isPremium: Boolean = false,
)

fun isAdminEmail(email: String?): Boolean {
    val admin = BuildConfig.ADMIN_EMAIL.trim().lowercase()
    return admin.isNotEmpty() && email != null && email.trim().lowercase() == admin
}

class AuthManager(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext

    private val firebaseReady: Boolean = try {
        FirebaseApp.getApps(appContext).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private val _state = MutableStateFlow(AuthState(configured = firebaseReady, loading = firebaseReady))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    private var expiryJob: Job? = null

    /** true while an account deletion is running, so the profile is not re-created. */
    @Volatile
    private var deleting = false

    init {
        if (firebaseReady) {
            FirebaseAuth.getInstance().addAuthStateListener { auth -> onUserChanged(auth.currentUser) }
        }
    }

    private fun onUserChanged(fbUser: FirebaseUser?) {
        profileListener?.remove()
        profileListener = null
        expiryJob?.cancel()

        if (fbUser == null) {
            _state.value = _state.value.copy(
                loading = false,
                user = null,
                isAdmin = false,
                premiumUntilMs = null,
                isPremium = false,
            )
            return
        }

        val email = fbUser.email ?: ""
        val appUser = AppUser(
            uid = fbUser.uid,
            email = email,
            name = fbUser.displayName ?: fbUser.email ?: "Learner",
        )
        _state.value = _state.value.copy(
            loading = false,
            user = appUser,
            isAdmin = isAdminEmail(email),
            premiumUntilMs = null,
            isPremium = false,
        )

        // Live read of users/{uid} so premium activates the moment the admin grants it.
        val ref = FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(fbUser.uid)
        profileListener = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "Profile listener failed", error)
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                if (deleting) return@addSnapshotListener
                // First login: create the profile. premiumUntil is never written by the client.
                val data = hashMapOf<String, Any>(
                    "email" to appUser.email,
                    "name" to appUser.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                ref.set(data).addOnFailureListener { e -> Log.e(TAG, "Could not create profile", e) }
                return@addSnapshotListener
            }
            applyPremium(snap.getTimestamp("premiumUntil")?.toDate()?.time)
        }
    }

    private fun applyPremium(untilMs: Long?) {
        expiryJob?.cancel()
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(
            premiumUntilMs = untilMs,
            isPremium = untilMs != null && untilMs > now,
        )
        // Re-evaluate exactly when premium expires so the app locks again without a restart.
        if (untilMs != null) {
            val remaining = untilMs - now
            if (remaining in 1..MAX_DELAY_MS) {
                expiryJob = scope.launch {
                    delay(remaining + 500)
                    applyPremium(untilMs)
                }
            }
        }
    }

    private suspend fun googleFirebaseCredential(activity: Activity): AuthCredential {
        if (!firebaseReady) throw IllegalStateException("Firebase is not configured yet.")
        val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (resId == 0) throw IllegalStateException("default_web_client_id is missing in google-services.json")
        val webClientId = activity.getString(resId)

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val manager = CredentialManager.create(activity)
        val result = try {
            manager.getCredential(activity, request)
        } catch (e: NoCredentialException) {
            // Seen after signing out: retry with the explicit "Sign in with Google" chooser.
            val buttonRequest = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
                .build()
            manager.getCredential(activity, buttonRequest)
        }
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAuthProvider.getCredential(google.idToken, null)
        }
        throw IllegalStateException("Unexpected credential type")
    }

    suspend fun signIn(activity: Activity) {
        val credential = googleFirebaseCredential(activity)
        FirebaseAuth.getInstance().signInWithCredential(credential).await()
    }

    /**
     * Deletes the profile document first, then the login account.
     * If Firebase wants a fresh login, Google sign-in is asked once more.
     */
    suspend fun deleteAccount(activity: Activity) {
        if (!firebaseReady) throw IllegalStateException("Firebase is not configured yet.")
        val user = FirebaseAuth.getInstance().currentUser ?: throw IllegalStateException("Not signed in")
        deleting = true
        try {
            FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(user.uid).delete().await()
            try {
                user.delete().await()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                user.reauthenticate(googleFirebaseCredential(activity)).await()
                user.delete().await()
            }
        } finally {
            deleting = false
        }
    }

    suspend fun signOut() {
        if (!firebaseReady) return
        FirebaseAuth.getInstance().signOut()
    }
}

// ---------------------------------------------------------------------------------------------
// Admin: find a user and set premiumUntil (real protection = Firestore rules, see README)
// ---------------------------------------------------------------------------------------------

data class FoundUser(val uid: String, val email: String, val name: String, val premiumUntilMs: Long?)

object AdminRepository {
    private fun toUser(doc: DocumentSnapshot): FoundUser = FoundUser(
        uid = doc.id,
        email = doc.getString("email").orEmpty(),
        name = doc.getString("name").orEmpty(),
        premiumUntilMs = doc.getTimestamp("premiumUntil")?.toDate()?.time,
    )

    /** User ID first, then email. Returns null when nobody matches. */
    suspend fun findUser(term: String): FoundUser? {
        val db = FirebaseFirestore.getInstance()
        val byId = try {
            db.collection(USERS_COLLECTION).document(term).get().await()
        } catch (e: IllegalArgumentException) {
            null
        }
        if (byId != null && byId.exists()) return toUser(byId)

        val snap = db.collection(USERS_COLLECTION).whereEqualTo("email", term).limit(1).get().await()
        val first = snap.documents.firstOrNull() ?: return null
        return toUser(first)
    }

    /** [untilMs] = null removes premium. */
    suspend fun writePremium(uid: String, untilMs: Long?) {
        val db = FirebaseFirestore.getInstance()
        val value: Any = if (untilMs != null) Timestamp(Date(untilMs)) else FieldValue.delete()
        db.collection(USERS_COLLECTION).document(uid).update("premiumUntil", value).await()
    }
}
