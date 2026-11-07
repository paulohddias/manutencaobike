package br.com.viavalesistemas.manutencaobike.data

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import br.com.viavalesistemas.manutencaobike.data.strava.StravaNaoConectadoException
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Rede(cause: Throwable) : DataError("Sem conexão. Verifique sua internet.", cause)
    class Servidor(val codigo: Int, cause: Throwable) : DataError("O servidor respondeu com erro ($codigo).", cause)
    class NaoConectado : DataError("Conecte sua conta do Strava novamente.")
    class EmUso(cause: Throwable) : DataError("Este item está em uso e não pode ser excluído.", cause)
    class Local(cause: Throwable) : DataError("Não foi possível salvar os dados.", cause)
    class Foto(cause: Throwable) : DataError("Não foi possível carregar a foto.", cause)
    class Backup(mensagem: String, cause: Throwable? = null) : DataError(mensagem, cause)
}

/** Fronteira de erros dos repositórios: exceções de plataforma viram [DataError]. */
suspend fun <T> comTratamentoDeErros(bloco: suspend () -> T): Result<T> =
    try {
        Result.success(bloco())
    } catch (e: CancellationException) {
        throw e
    } catch (e: StravaNaoConectadoException) {
        Result.failure(DataError.NaoConectado())
    } catch (e: IOException) {
        Result.failure(DataError.Rede(e))
    } catch (e: HttpException) {
        Result.failure(if (e.code() == 401) DataError.NaoConectado() else DataError.Servidor(e.code(), e))
    } catch (e: SQLiteConstraintException) {
        Result.failure(DataError.EmUso(e))
    } catch (e: SQLiteException) {
        Result.failure(DataError.Local(e))
    }
