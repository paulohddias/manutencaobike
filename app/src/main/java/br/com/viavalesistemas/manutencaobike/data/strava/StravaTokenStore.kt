package br.com.viavalesistemas.manutencaobike.data.strava

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class StravaTokens(val accessToken: String, val refreshToken: String, val expiraEm: Long)

@Singleton
class StravaTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val conectado: Flow<Boolean> = dataStore.data.map { it[REFRESH] != null }
    val nomeAtleta: Flow<String?> = dataStore.data.map { it[ATLETA] }

    suspend fun tokens(): StravaTokens? {
        val prefs = dataStore.data.first()
        val access = prefs[ACCESS] ?: return null
        val refresh = prefs[REFRESH] ?: return null
        return StravaTokens(access, refresh, prefs[EXPIRA] ?: 0L)
    }

    suspend fun salvar(token: TokenDto) {
        dataStore.edit { prefs ->
            prefs[ACCESS] = token.accessToken
            prefs[REFRESH] = token.refreshToken
            prefs[EXPIRA] = token.expiresAt
            token.athlete?.let { atleta ->
                listOfNotNull(atleta.firstname, atleta.lastname).joinToString(" ").ifBlank { null }
                    ?.let { prefs[ATLETA] = it }
            }
        }
    }

    suspend fun limpar() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS)
            prefs.remove(REFRESH)
            prefs.remove(EXPIRA)
            prefs.remove(ATLETA)
        }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("strava_access_token")
        val REFRESH = stringPreferencesKey("strava_refresh_token")
        val EXPIRA = longPreferencesKey("strava_expira_em")
        val ATLETA = stringPreferencesKey("strava_atleta")
    }
}
