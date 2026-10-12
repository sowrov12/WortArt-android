package com.sowrov.germanarticlebangla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sowrov.germanarticlebangla.ui.AppViewModel
import com.sowrov.germanarticlebangla.ui.WortArtRoot

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { WortArtRoot(viewModel) }
    }
}
