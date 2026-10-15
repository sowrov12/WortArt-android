package com.sowrov.germanarticlebangla.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

object AppIcons {
    val Add: ImageVector = Icons.Filled.Add
    val Admin: ImageVector = Icons.Filled.AdminPanelSettings
    val Audio: ImageVector = Icons.Filled.GraphicEq
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Bell: ImageVector = Icons.Filled.Notifications
    val Book: ImageVector = Icons.AutoMirrored.Filled.MenuBook
    val Bulb: ImageVector = Icons.Filled.Lightbulb
    val Chart: ImageVector = Icons.Filled.BarChart
    val Chat: ImageVector = Icons.Filled.Chat
    val Check: ImageVector = Icons.Filled.Check
    val ChevronDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val ChevronRight: ImageVector = Icons.Filled.ChevronRight
    val Clock: ImageVector = Icons.Filled.Schedule
    val Close: ImageVector = Icons.Filled.Close
    val Copy: ImageVector = Icons.Filled.ContentCopy
    val Crown: ImageVector = Icons.Filled.WorkspacePremium
    val Delete: ImageVector = Icons.Filled.Delete
    val Edit: ImageVector = Icons.Filled.Edit
    val Favorite: ImageVector = Icons.Filled.Favorite
    val FavoriteOutline: ImageVector = Icons.Filled.FavoriteBorder
    val Flame: ImageVector = Icons.Filled.LocalFireDepartment
    val Forward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    val Grid: ImageVector = Icons.Filled.GridView
    val Infinity: ImageVector = Icons.Filled.AllInclusive
    val Layers: ImageVector = Icons.Filled.Layers
    val Lock: ImageVector = Icons.Filled.Lock
    val Logout: ImageVector = Icons.AutoMirrored.Filled.Logout
    val Moon: ImageVector = Icons.Filled.DarkMode
    val Mic: ImageVector = Icons.Filled.Mic
    val More: ImageVector = Icons.Filled.MoreVert
    val Phone: ImageVector = Icons.Filled.PhoneAndroid
    val Person: ImageVector = Icons.Filled.Person
    val School: ImageVector = Icons.Filled.School
    val Search: ImageVector = Icons.Filled.Search
    val Skip: ImageVector = Icons.Filled.SkipNext
    val Sun: ImageVector = Icons.Filled.LightMode
    val ThemeMix: ImageVector = Icons.Filled.Brightness6
    val Target: ImageVector = Icons.Filled.TrackChanges
    val Trophy: ImageVector = Icons.Filled.EmojiEvents
    val Volume: ImageVector = Icons.AutoMirrored.Filled.VolumeUp

    /** Icon for a deck/category id (the original 16 keep a matching icon; new sheet categories use Layers). */
    fun forCategory(id: String): ImageVector = when (id) {
        "all" -> Icons.Filled.Layers
        "favorites" -> Icons.Filled.Favorite
        "food" -> Icons.Filled.Restaurant
        "family" -> Icons.Filled.Groups
        "home" -> Icons.Filled.Home
        "daily" -> Icons.Filled.WbSunny
        "education" -> Icons.Filled.School
        "work" -> Icons.Filled.Work
        "business" -> Icons.Filled.Business
        "travel" -> Icons.Filled.Flight
        "health" -> Icons.Filled.HealthAndSafety
        "shopping" -> Icons.Filled.ShoppingBag
        "time" -> Icons.Filled.CalendarMonth
        "nature" -> Icons.Filled.Cloud
        "government" -> Icons.Filled.AccountBalance
        "technology" -> Icons.Filled.Laptop
        "communication" -> Icons.Filled.Chat
        "society" -> Icons.Filled.Public
        else -> Icons.Filled.Layers
    }
}
