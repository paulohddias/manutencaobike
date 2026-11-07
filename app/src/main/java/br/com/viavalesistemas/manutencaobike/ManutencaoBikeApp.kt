package br.com.viavalesistemas.manutencaobike

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.viavalesistemas.manutencaobike.alertas.Notificador
import br.com.viavalesistemas.manutencaobike.alertas.VerificacaoWorker
import br.com.viavalesistemas.manutencaobike.data.backup.BackupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ManutencaoBikeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notificador: Notificador

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notificador.criarCanal()
        VerificacaoWorker.agendar(this)
        BackupWorker.agendar(this)
    }
}
