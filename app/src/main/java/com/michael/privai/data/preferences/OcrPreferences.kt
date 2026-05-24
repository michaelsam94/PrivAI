package com.michael.privai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ocrDataStore: DataStore<Preferences> by preferencesDataStore(name = "ocr_settings")

class OcrPreferences(context: Context) {

    private val dataStore = context.applicationContext.ocrDataStore

    val highAccuracyEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HIGH_ACCURACY_OCR] ?: false
    }

    suspend fun setHighAccuracyEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HIGH_ACCURACY_OCR] = enabled
        }
    }

    private companion object {
        val KEY_HIGH_ACCURACY_OCR = booleanPreferencesKey("high_accuracy_ocr")
    }
}
