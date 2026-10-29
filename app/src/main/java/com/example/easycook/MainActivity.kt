package com.example.easycook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.easycook.data.RecipeRepository
import com.example.easycook.navigation.EasyCookNavHost
import com.example.easycook.transfer.ContentResolverDocuments
import com.example.easycook.transfer.TextDocuments
import com.example.easycook.ui.theme.EasyCookTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val repository = (application as EasyCookApplication).repository

        setContent {
            EasyCookTheme {
                EasyCookApp(
                    repository = repository,
                    documents = remember { ContentResolverDocuments(applicationContext) }
                )
            }
        }
    }
}

@Composable
fun EasyCookApp(repository: RecipeRepository, documents: TextDocuments) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        EasyCookNavHost(
            repository = repository,
            documents = documents,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        )
    }
}
