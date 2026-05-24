package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.OCRScreen
import com.example.ui.screens.TranscriptionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PrivAIViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PrivAIAppNavigation()
            }
        }
    }
}

@Composable
fun PrivAIAppNavigation() {
    val navController = rememberNavController()
    val viewModel: PrivAIViewModel = viewModel()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTranscribe = {
                        navController.navigate("transcribe")
                    },
                    onNavigateToOCR = { uri ->
                        if (uri != null) {
                            val encodedUri = Uri.encode(uri.toString())
                            navController.navigate("ocr?imageUri=$encodedUri")
                        } else {
                            navController.navigate("ocr")
                        }
                    },
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate("note_detail/$noteId")
                    }
                )
            }

            composable("transcribe") {
                TranscriptionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "ocr?imageUri={imageUri}",
                arguments = listOf(
                    navArgument("imageUri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri")
                OCRScreen(
                    viewModel = viewModel,
                    imageUriString = imageUri,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                NoteDetailScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
