package com.michael.privai.playstore

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import com.michael.privai.R
import com.michael.privai.ui.theme.MyApplicationTheme
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreIconTest {

  @Test
  @Config(qualifiers = "w512dp-h512dp-mdpi")
  fun app_icon_512() {
    val appContext = ApplicationProvider.getApplicationContext<Context>()
    capturePlayStoreImage("app-icon-512.png") {
      MyApplicationTheme {
        val iconBitmap = remember {
          val drawable = checkNotNull(appContext.getDrawable(R.mipmap.ic_launcher))
          drawable.toBitmap(512, 512).asImageBitmap()
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier.size(480.dp),
          )
        }
      }
    }
  }
}
