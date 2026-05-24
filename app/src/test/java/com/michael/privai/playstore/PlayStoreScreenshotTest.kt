package com.michael.privai.playstore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PHONE = "w360dp-h640dp-xxhdpi"
private const val TABLET = "w800dp-h1280dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {

  private val app: Application
    get() = ApplicationProvider.getApplicationContext()

  @After
  fun tearDown() {
    teardownPlayStoreDatabase()
  }

  @Test
  @Config(qualifiers = PHONE)
  fun phone_01_dashboard() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/01_dashboard.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.Dashboard, viewModel)
    }
  }

  @Test
  @Config(qualifiers = PHONE)
  fun phone_02_search() {
    val viewModel = createSeededPlayStoreViewModel(app)
    viewModel.updateSearchQuery(PlayStoreTestFixtures.SEARCH_QUERY)
    capturePlayStoreImage("phone/02_search.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.Dashboard, viewModel)
    }
  }

  @Test
  @Config(qualifiers = PHONE)
  fun phone_03_add_note() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/03_add_note.png") {
      PlayStoreAddNoteOverlay(viewModel)
    }
  }

  @Test
  @Config(qualifiers = PHONE)
  fun phone_04_note_detail() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("phone/04_note_detail.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.NoteDetail, viewModel)
    }
  }

  @Test
  @Config(qualifiers = TABLET)
  fun tablet_01_dashboard() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/01_dashboard.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.Dashboard, viewModel)
    }
  }

  @Test
  @Config(qualifiers = TABLET)
  fun tablet_02_search() {
    val viewModel = createSeededPlayStoreViewModel(app)
    viewModel.updateSearchQuery(PlayStoreTestFixtures.SEARCH_QUERY)
    capturePlayStoreImage("tablet/02_search.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.Dashboard, viewModel)
    }
  }

  @Test
  @Config(qualifiers = TABLET)
  fun tablet_03_add_note() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/03_add_note.png") {
      PlayStoreAddNoteOverlay(viewModel)
    }
  }

  @Test
  @Config(qualifiers = TABLET)
  fun tablet_04_note_detail() {
    val viewModel = createSeededPlayStoreViewModel(app)
    capturePlayStoreImage("tablet/04_note_detail.png") {
      PlayStoreScreenshotFrame(PlayStoreScene.NoteDetail, viewModel)
    }
  }
}
