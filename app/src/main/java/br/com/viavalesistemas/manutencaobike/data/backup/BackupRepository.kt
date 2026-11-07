package br.com.viavalesistemas.manutencaobike.data.backup

import android.content.Context
import br.com.viavalesistemas.manutencaobike.data.DataError
import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.servidor.BackupDto
import br.com.viavalesistemas.manutencaobike.data.servidor.VvsApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaTokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class BackupRemoto(val id: String, val criadoEm: Instant, val tamanhoBytes: Long)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pacote: PacoteBackup,
    private val api: VvsApi,
    private val preferencias: BackupPreferencias,
    private val tokenStore: StravaTokenStore,
) {
    val automatico: Flow<Boolean> = preferencias.automatico
    val ultimoBackupEm: Flow<Instant?> = preferencias.ultimoBackupEm

    suspend fun definirAutomatico(ativo: Boolean) = preferencias.definirAutomatico(ativo)

    /** Devolve false quando [somenteSeMudou] e nada mudou desde o último envio. */
    suspend fun fazerBackup(somenteSeMudou: Boolean = false): Result<Boolean> = tratar {
        val zip = File(context.cacheDir, "backup_envio.zip")
        try {
            val assinatura = pacote.gerar(zip)
            if (somenteSeMudou && assinatura == preferencias.ultimaAssinatura()) return@tratar false
            api.enviarBackup(zip.asRequestBody(TIPO_ZIP))
            preferencias.registrarEnvio(assinatura, Instant.now())
            true
        } finally {
            zip.delete()
        }
    }

    suspend fun listar(): Result<List<BackupRemoto>> = tratar {
        api.listarBackups().backups.map(BackupDto::toModel)
    }

    /** Depois do sucesso o app precisa ser reiniciado para aplicar a restauração. */
    suspend fun restaurar(id: String): Result<Unit> = tratar {
        val zip = File(context.cacheDir, "backup_restauracao.zip")
        try {
            val corpo = api.baixarBackup(id)
            withContext(Dispatchers.IO) {
                corpo.use { resposta -> zip.outputStream().use { resposta.byteStream().copyTo(it) } }
            }
            pacote.prepararRestauracao(zip)
        } finally {
            zip.delete()
        }
    }

    /** Apaga todos os backups e sessões do usuário no servidor e desconecta o app. */
    suspend fun excluirDadosDoServidor(): Result<Unit> = tratar {
        api.excluirConta()
        tokenStore.limpar()
        preferencias.limpar()
    }

    private suspend fun <T> tratar(bloco: suspend () -> T): Result<T> {
        val resultado = try {
            comTratamentoDeErros(bloco)
        } catch (e: BackupException) {
            Result.failure(DataError.Backup(e.message ?: "Falha no backup.", e))
        }
        return resultado.recoverCatching { erro ->
            throw when {
                erro is DataError.Servidor && erro.codigo == 413 -> DataError.Backup("Backup grande demais para o servidor.", erro)
                erro is DataError.Servidor && erro.codigo == 429 -> DataError.Backup("Aguarde um minuto antes de enviar outro backup.", erro)
                erro is DataError.Servidor && erro.codigo == 404 -> DataError.Backup("Backup não encontrado no servidor.", erro)
                else -> erro
            }
        }
    }

    private companion object {
        // O ModSecurity da HostGator responde 406 a corpos "application/zip"; octet-stream passa.
        val TIPO_ZIP = "application/octet-stream".toMediaType()
    }
}

private fun BackupDto.toModel() = BackupRemoto(id, Instant.ofEpochSecond(criadoEm), tamanho)
