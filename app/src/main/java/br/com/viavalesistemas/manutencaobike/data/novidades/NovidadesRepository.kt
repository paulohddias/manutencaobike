package br.com.viavalesistemas.manutencaobike.data.novidades

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.model.NotasVersao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Notas de versão do assets/novidades.json e controle de qual versão o usuário já viu. */
@Singleton
class NovidadesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private var cache: List<NotasVersao>? = null

    /** Todas as versões, da mais nova para a mais antiga. */
    suspend fun todas(): List<NotasVersao> = cache ?: withContext(Dispatchers.IO) {
        val texto = context.assets.open(ARQUIVO).bufferedReader().use { it.readText() }
        lerNotas(json, texto).also { cache = it }
    }

    /** O que mostrar depois de uma atualização; vazio na primeira instalação ou se já foi visto. */
    suspend fun pendentes(): List<NotasVersao> {
        val atual = BuildConfig.VERSION_CODE
        val vista = dataStore.data.first()[ULTIMA_VISTA]
        val desde = when {
            vista != null -> vista
            instalacaoNova() -> {
                marcarVistas()
                return emptyList()
            }
            // Atualizou de uma versão anterior a este recurso: mostra só a atual.
            else -> atual - 1
        }
        return todas().filter { it.codigo in (desde + 1)..atual && !it.vazia }
    }

    suspend fun marcarVistas() {
        dataStore.edit { it[ULTIMA_VISTA] = BuildConfig.VERSION_CODE }
    }

    private fun instalacaoNova(): Boolean {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.firstInstallTime == info.lastUpdateTime
    }

    companion object {
        const val ARQUIVO = "novidades.json"
        private val ULTIMA_VISTA = intPreferencesKey("novidades_ultima_versao_vista")

        fun lerNotas(json: Json, texto: String): List<NotasVersao> =
            json.decodeFromString(ListSerializer(NotasVersao.serializer()), texto).sortedByDescending { it.codigo }
    }
}
