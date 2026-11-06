package br.com.viavalesistemas.manutencaobike.alertas

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertasNotificadosStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun chaves(): Set<String> = dataStore.data.first()[CHAVES].orEmpty()

    suspend fun substituir(chaves: Set<String>) {
        dataStore.edit { it[CHAVES] = chaves }
    }

    private companion object {
        val CHAVES = stringSetPreferencesKey("alertas_notificados")
    }
}
