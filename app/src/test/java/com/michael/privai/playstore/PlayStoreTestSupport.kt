package com.michael.privai.playstore

import android.app.Application
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import com.michael.privai.data.database.PrivAIDatabase
import com.michael.privai.ui.viewmodel.PrivAIViewModel
import kotlinx.coroutines.runBlocking

fun ComposeContentTestRule.prepareForScreenshot() {
  mainClock.autoAdvance = true
  waitForIdle()
  mainClock.advanceTimeByFrame()
  waitForIdle()
}

fun ComposeContentTestRule.waitForPlayStoreDashboard() {
  waitForIdle()
  mainClock.advanceTimeBy(500)
  waitForIdle()
  onNodeWithTag("note_card_${PlayStoreTestFixtures.NOTE_ID_DETAIL}", useUnmergedTree = true).assertExists()
}

fun createSeededPlayStoreViewModel(application: Application): PrivAIViewModel {
  PrivAIDatabase.resetForTests()
  PrivAIDatabase.inMemoryForTests = Room.inMemoryDatabaseBuilder(
    application,
    PrivAIDatabase::class.java,
  )
    .allowMainThreadQueries()
    .build()

  val viewModel = PrivAIViewModel(application)
  runBlocking {
    PlayStoreTestFixtures.seed(viewModel)
  }
  return viewModel
}

fun teardownPlayStoreDatabase() {
  PrivAIDatabase.resetForTests()
}
