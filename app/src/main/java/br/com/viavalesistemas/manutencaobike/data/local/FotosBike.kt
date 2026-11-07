package br.com.viavalesistemas.manutencaobike.data.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** Guarda as fotos das bikes na área privada do app, já reduzidas e com a rotação do EXIF aplicada. */
@Singleton
class FotosBike @Inject constructor(@ApplicationContext private val context: Context) {

    private val pasta: File get() = pastaFotos(context).apply { mkdirs() }

    fun listar(): List<File> = pasta.listFiles { arquivo -> NOME_VALIDO.matches(arquivo.name) }.orEmpty().toList()

    fun arquivo(nome: String): File = File(pasta, nome)

    /** Copia a imagem do [uri] (galeria ou câmera) e devolve o nome do arquivo salvo. */
    suspend fun importar(uri: Uri): String {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(LADO_MAXIMO)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        val resultado = context.imageLoader.execute(request) as? SuccessResult
            ?: throw IOException("Não foi possível ler a imagem")
        return withContext(Dispatchers.IO) {
            val bitmap = resultado.image.toBitmap().reduzida()
            val nome = "bike_${UUID.randomUUID()}.jpg"
            arquivo(nome).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, it) }
            nome
        }
    }

    fun excluir(nome: String) {
        arquivo(nome).delete()
    }

    /** Arquivo temporário onde o app de câmera grava a foto tirada. */
    fun uriParaCamera(): Uri {
        val dir = File(context.cacheDir, PASTA_CAMERA).apply { mkdirs() }
        val arquivo = File(dir, "captura_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.arquivos", arquivo)
    }

    fun limparCapturas() {
        File(context.cacheDir, PASTA_CAMERA).listFiles()?.forEach(File::delete)
    }

    // Precision.INEXACT só reduz por potências de 2, então pode sobrar até o dobro do tamanho.
    private fun Bitmap.reduzida(): Bitmap {
        val maior = max(width, height)
        if (maior <= LADO_MAXIMO) return this
        val fator = LADO_MAXIMO.toFloat() / maior
        return scale((width * fator).roundToInt(), (height * fator).roundToInt())
    }

    companion object {
        val NOME_VALIDO = Regex("bike_[0-9a-f-]{36}\\.jpg")

        fun pastaFotos(context: Context): File = File(context.filesDir, PASTA)

        private const val PASTA = "fotos"
        private const val PASTA_CAMERA = "camera"
        private const val LADO_MAXIMO = 1600
        private const val QUALIDADE = 85
    }
}
