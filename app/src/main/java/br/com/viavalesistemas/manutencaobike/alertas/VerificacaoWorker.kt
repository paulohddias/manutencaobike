package br.com.viavalesistemas.manutencaobike.alertas

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.viavalesistemas.manutencaobike.data.repositorio.StravaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Uma vez por dia: atualiza o km das bikes no Strava e notifica peças/serviços no limite. */
@HiltWorker
class VerificacaoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stravaRepository: StravaRepository,
    private val verificador: VerificadorAlertas,
    private val notificador: Notificador,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Sem internet, ainda vale verificar com o último km conhecido.
        if (stravaRepository.conectado.first()) stravaRepository.sincronizarBikes()

        return try {
            notificador.notificar(verificador.alertasNovos())
            Result.success()
        } catch (e: SQLiteException) {
            Result.retry()
        }
    }

    companion object {
        private const val NOME = "verificacao-manutencao"

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<VerificacaoWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }
    }
}
