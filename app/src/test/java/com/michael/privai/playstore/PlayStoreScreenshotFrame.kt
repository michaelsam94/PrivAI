package com.michael.privai.playstore

import androidx.compose.runtime.Composable
import com.michael.privai.ui.screens.HomeScreen
import com.michael.privai.ui.screens.NoteDetailScreen
import com.michael.privai.ui.theme.MyApplicationTheme
import com.michael.privai.ui.viewmodel.PrivAIViewModel

enum class PlayStoreScene {
  Dashboard,
  Search,
  AddNote,
  NoteDetail,
}

@Composable
fun PlayStoreScreenshotFrame(
  scene: PlayStoreScene,
  viewModel: PrivAIViewModel,
) {
  MyApplicationTheme {
    when (scene) {
      PlayStoreScene.Dashboard -> HomeScreen(
        viewModel = viewModel,
        onNavigateToTranscribe = {},
        onNavigateToOCR = {},
        onNavigateToNoteDetail = {},
      )
      PlayStoreScene.Search -> HomeScreen(
        viewModel = viewModel,
        onNavigateToTranscribe = {},
        onNavigateToOCR = {},
        onNavigateToNoteDetail = {},
      )
      PlayStoreScene.AddNote -> PlayStoreAddNoteOverlay(viewModel)
      PlayStoreScene.NoteDetail -> NoteDetailScreen(
        viewModel = viewModel,
        noteId = PlayStoreTestFixtures.NOTE_ID_DETAIL,
        onNavigateBack = {},
      )
    }
  }
}
