package br.com.viavalesistemas.manutencaobike.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.viavalesistemas.manutencaobike.data.DataError
import br.com.viavalesistemas.manutencaobike.data.strava.StravaTokenStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Backup automático diário, só quando algo mudou desde o último envio. */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val tokenStore: StravaTokenStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!backupRepository.automatico.first() || !tokenStore.conectado.first()) return Result.success()
        return backupRepository.fazerBackup(somenteSeMudou = true).fold(
            onSuccess = { Result.success() },
            // Só falta de rede vale tentar de novo; outros erros esperam o próximo dia.
            onFailure = { if (it is DataError.Rede && runAttemptCount < 3) Result.retry() else Result.success() },
        )
    }

    companion object {
        private const val NOME = "backup-automatico"

        fun agendar(context: Context) {
            val restricoes = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val pedido = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(restricoes)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }
    }
}
