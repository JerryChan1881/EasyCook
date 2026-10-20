package com.example.easycook

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.example.easycook.ui.theme.EasyCookTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            EasyCookTheme {
                EasyCookApp()
            }
        }
    }
}



@Composable
fun EasyCookApp() {

    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val stateViewModel: StateViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StateViewModel.from(application) }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHostModel(
            navController = navController,
            stateViewModel = stateViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}