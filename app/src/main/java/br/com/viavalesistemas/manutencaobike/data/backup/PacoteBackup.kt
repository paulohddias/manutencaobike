package br.com.viavalesistemas.manutencaobike.data.backup

import android.content.Context
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.local.AppDatabase
import br.com.viavalesistemas.manutencaobike.data.local.FotosBike
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Manifesto(
    val formato: Int,
    val versaoBanco: Int,
    val criadoEm: Long,
    val appVersao: String,
)

class BackupException(mensagem: String, cause: Throwable? = null) : Exception(mensagem, cause)

/**
 * Formato do backup: um .zip com manifesto.json, banco.db (o SQLite do Room) e fotos/.
 * A restauração é feita em duas etapas: o zip é validado e extraído para uma pasta pendente, e os
 * arquivos só são trocados na próxima abertura do app, antes de o Room abrir o banco.
 */
@Singleton
class PacoteBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val fotos: FotosBike,
    private val json: Json,
) {

    /** Gera o zip em [destino] e devolve uma assinatura do conteúdo, usada para pular backups repetidos. */
    suspend fun gerar(destino: File): String = withContext(Dispatchers.IO) {
        try {
            // Leva para o arquivo principal o que ainda está no WAL, senão a cópia sai desatualizada.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val manifesto = Manifesto(FORMATO, AppDatabase.VERSAO, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
            val digest = MessageDigest.getInstance("SHA-256")
            ZipOutputStream(destino.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFESTO))
                zip.write(json.encodeToString(Manifesto.serializer(), manifesto).toByteArray())
                zip.closeEntry()
                zip.adicionar(BANCO, context.getDatabasePath(AppDatabase.NOME), digest)
                fotos.listar().sortedBy { it.name }.forEach { foto ->
                    digest.update(foto.name.toByteArray())
                    zip.adicionar("$PASTA_FOTOS/${foto.name}", foto, digest)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: IOException) {
            throw BackupException("Não foi possível gerar o backup.", e)
        }
    }

    /** Valida o zip baixado e deixa a restauração pronta para a próxima abertura do app. */
    suspend fun prepararRestauracao(zip: File) = withContext(Dispatchers.IO) {
        val pendente = File(context.filesDir, PASTA_PENDENTE)
        pendente.deleteRecursively()
        File(pendente, PASTA_FOTOS).mkdirs()
        try {
            val manifesto = extrair(zip, pendente)
            if (manifesto.versaoBanco > AppDatabase.VERSAO) {
                throw BackupException("Este backup foi feito numa versão mais nova do app. Atualize o app para restaurar.")
            }
            if (!File(pendente, BANCO).pareceSqlite()) throw BackupException("O banco de dados do backup está corrompido.")
            File(pendente, MARCADOR).createNewFile()
        } catch (e: Exception) {
            pendente.deleteRecursively()
            throw when (e) {
                is BackupException -> e
                is IOException, is SerializationException -> BackupException("Arquivo de backup inválido.", e)
                else -> e
            }
        }
    }

    private fun extrair(zip: File, pendente: File): Manifesto {
        var manifesto: Manifesto? = null
        var temBanco = false
        var restante = LIMITE_DESCOMPACTADO
        ZipInputStream(zip.inputStream().buffered()).use { entrada ->
            while (true) {
                val item = entrada.nextEntry ?: break
                val nomeFoto = item.name.removePrefix("$PASTA_FOTOS/")
                // Só nomes conhecidos são extraídos: evita sobrescrever arquivos fora da pasta (zip slip).
                val destino = when {
                    item.isDirectory -> null
                    item.name == MANIFESTO -> {
                        manifesto = json.decodeFromString(Manifesto.serializer(), entrada.lerTexto(LIMITE_MANIFESTO))
                        null
                    }
                    item.name == BANCO -> File(pendente, BANCO).also { temBanco = true }
                    item.name.startsWith("$PASTA_FOTOS/") && FotosBike.NOME_VALIDO.matches(nomeFoto) ->
                        File(pendente, "$PASTA_FOTOS/$nomeFoto")
                    else -> null
                }
                if (destino != null) restante -= destino.outputStream().use { entrada.copiarComLimite(it, restante) }
            }
        }
        if (!temBanco) throw BackupException("O backup não contém o banco de dados.")
        return manifesto ?: throw BackupException("O backup não contém o manifesto.")
    }

    private fun ZipOutputStream.adicionar(nome: String, arquivo: File, digest: MessageDigest) {
        putNextEntry(ZipEntry(nome))
        arquivo.inputStream().use { entrada ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val lidos = entrada.read(buffer)
                if (lidos < 0) break
                digest.update(buffer, 0, lidos)
                write(buffer, 0, lidos)
            }
        }
        closeEntry()
    }

    companion object {
        private const val FORMATO = 1
        private const val MANIFESTO = "manifesto.json"
        private const val BANCO = "banco.db"
        private const val PASTA_FOTOS = "fotos"
        private const val PASTA_PENDENTE = "restauracao_pendente"
        private const val MARCADOR = "pronto"
        private const val BUFFER = 64 * 1024
        private const val LIMITE_MANIFESTO = 64 * 1024L
        // Proteção contra zip bomb: nenhum backup legítimo chega perto disso.
        private const val LIMITE_DESCOMPACTADO = 500L * 1024 * 1024

        /** Chamado antes de o Room abrir o banco. Se cair no meio, o marcador continua e tenta de novo. */
        fun aplicarRestauracaoPendente(context: Context) {
            val pendente = File(context.filesDir, PASTA_PENDENTE)
            val marcador = File(pendente, MARCADOR)
            if (!marcador.exists()) {
                pendente.deleteRecursively()
                return
            }
            val banco = context.getDatabasePath(AppDatabase.NOME)
            banco.parentFile?.mkdirs()
            listOf("-wal", "-shm", "-journal").forEach { File(banco.path + it).delete() }
            File(pendente, BANCO).copyTo(banco, overwrite = true)

            val pastaFotos = FotosBike.pastaFotos(context)
            pastaFotos.deleteRecursively()
            pastaFotos.mkdirs()
            File(pendente, PASTA_FOTOS).listFiles()?.forEach { it.copyTo(File(pastaFotos, it.name), overwrite = true) }

            marcador.delete()
            pendente.deleteRecursively()
        }

        private fun File.pareceSqlite(): Boolean =
            length() >= 16 && inputStream().use { String(it.readNBytes16(), Charsets.US_ASCII) } == "SQLite format 3\u0000"

        private fun InputStream.readNBytes16(): ByteArray {
            val bytes = ByteArray(16)
            var lidos = 0
            while (lidos < 16) {
                val n = read(bytes, lidos, 16 - lidos)
                if (n < 0) break
                lidos += n
            }
            return bytes
        }

        private fun InputStream.lerTexto(limite: Long): String =
            java.io.ByteArrayOutputStream().also { copiarComLimite(it, limite) }.toString(Charsets.UTF_8.name())

        private fun InputStream.copiarComLimite(saida: OutputStream, limite: Long): Long {
            val buffer = ByteArray(BUFFER)
            var total = 0L
            while (true) {
                val lidos = read(buffer)
                if (lidos < 0) break
                total += lidos
                if (total > limite) throw BackupException("Backup grande demais.")
                saida.write(buffer, 0, lidos)
            }
            return total
        }
    }
}
