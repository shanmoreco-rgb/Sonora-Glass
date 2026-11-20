package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SonoraThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sonoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonora_prefs")

data class LocalSonoraPrefs(
    val themeMode: SonoraThemeMode = SonoraThemeMode.LIQUID_GLASS,
    val offlineModeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val hiResOutputEnabled: Boolean = true,
    val activeEqPresetId: String = "preset_studio_flat"
)

class SonoraPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val GAPLESS = booleanPreferencesKey("gapless")
        val HIRES_OUTPUT = booleanPreferencesKey("hires_output")
        val ACTIVE_EQ_PRESET = stringPreferencesKey("active_eq_preset")
    }

    val prefsFlow: Flow<LocalSonoraPrefs> = context.sonoraDataStore.data.map { prefs ->
        LocalSonoraPrefs(
            themeMode = SonoraThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            offlineModeEnabled = prefs[Keys.OFFLINE_MODE] ?: false,
            gaplessEnabled = prefs[Keys.GAPLESS] ?: true,
            hiResOutputEnabled = prefs[Keys.HIRES_OUTPUT] ?: true,
            activeEqPresetId = prefs[Keys.ACTIVE_EQ_PRESET] ?: "preset_studio_flat"
        )
    }

    suspend fun setThemeMode(mode: SonoraThemeMode) {
        context.sonoraDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.sonoraDataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.sonoraDataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setHiResOutputEnabled(enabled: Boolean) {
        context.sonoraDataStore.edit { it[Keys.HIRES_OUTPUT] = enabled }
    }

    suspend fun setActiveEqPresetId(presetId: String) {
        context.sonoraDataStore.edit { it[Keys.ACTIVE_EQ_PRESET] = presetId }
    }
}
