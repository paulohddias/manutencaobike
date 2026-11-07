package br.com.viavalesistemas.manutencaobike.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupPreferencias @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val automatico: Flow<Boolean> = dataStore.data.map { it[AUTOMATICO] ?: true }
    val ultimoBackupEm: Flow<Instant?> = dataStore.data.map { prefs -> prefs[ULTIMO]?.let(Instant::ofEpochMilli) }

    suspend fun definirAutomatico(ativo: Boolean) {
        dataStore.edit { it[AUTOMATICO] = ativo }
    }

    suspend fun ultimaAssinatura(): String? = dataStore.data.first()[ASSINATURA]

    suspend fun registrarEnvio(assinatura: String, em: Instant) {
        dataStore.edit {
            it[ASSINATURA] = assinatura
            it[ULTIMO] = em.toEpochMilli()
        }
    }

    suspend fun limpar() {
        dataStore.edit {
            it.remove(ASSINATURA)
            it.remove(ULTIMO)
        }
    }

    private companion object {
        val AUTOMATICO = booleanPreferencesKey("backup_automatico")
        val ULTIMO = longPreferencesKey("backup_ultimo_em")
        val ASSINATURA = stringPreferencesKey("backup_assinatura")
    }
}
