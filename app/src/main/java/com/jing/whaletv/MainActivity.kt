package com.jing.whaletv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jing.whaletv.ui.MainViewModel
import com.jing.whaletv.ui.MainViewModelFactory
import com.jing.whaletv.ui.screens.WhaleTvRoot
import com.jing.whaletv.ui.theme.WhaleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WhaleTvApp).container
        setContent {
            WhaleTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(container))
                WhaleTvRoot(viewModel)
            }
        }
    }
}
