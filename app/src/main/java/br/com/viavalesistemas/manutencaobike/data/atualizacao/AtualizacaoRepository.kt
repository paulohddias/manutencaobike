package br.com.viavalesistemas.manutencaobike.data.atualizacao

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/** Cadastro de versões da VVS (sistemavvs.com.br/versao-app), compartilhado com outros apps da empresa. */
interface VersaoApi {
    @GET("versao-app/api.php")
    suspend fun versao(@Query("dispositivo") dispositivo: String): VersaoDto
}

/** Resposta de api.php; dispositivo inexistente volta 200 com só o campo "erro". */
@Serializable
data class VersaoDto(
    @SerialName("versao_app") val versaoApp: String? = null,
    val erro: String? = null,
)

@Singleton
class AtualizacaoRepository @Inject constructor(
    private val api: VersaoApi,
    private val dataStore: DataStore<Preferences>,
) {
    /**
     * versionCode publicado no site quando for maior que o instalado e o usuário ainda não
     * dispensou o aviso dessa versão; null em qualquer outro caso (inclusive sem internet).
     */
    suspend fun versaoDisponivel(): Int? {
        val publicada = comTratamentoDeErros { api.versao(DISPOSITIVO).versaoApp?.trim()?.toIntOrNull() }
            .getOrNull() ?: return null
        val dispensada = dataStore.data.first()[DISPENSADA] ?: 0
        return publicada.takeIf { it > BuildConfig.VERSION_CODE && it > dispensada }
    }

    suspend fun dispensar(versao: Int) {
        dataStore.edit { it[DISPENSADA] = versao }
    }

    companion object {
        /** Tag deste app no cadastro de versões; "versao_app" lá deve ser o versionCode. */
        const val DISPOSITIVO = "manutencaobike"
        const val URL_BASE = "https://sistemavvs.com.br/"
        private val DISPENSADA = intPreferencesKey("atualizacao_versao_dispensada")
    }
}
