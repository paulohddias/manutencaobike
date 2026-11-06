package br.com.viavalesistemas.manutencaobike.alertas

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.viavalesistemas.manutencaobike.MainActivity
import br.com.viavalesistemas.manutencaobike.R
import br.com.viavalesistemas.manutencaobike.model.Alerta
import br.com.viavalesistemas.manutencaobike.model.OrigemAlerta
import br.com.viavalesistemas.manutencaobike.model.StatusDesgaste
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt

class Notificador @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun criarCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL_ID, "Alertas de manutenção", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos de peças gastas e serviços vencidos"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    @SuppressLint("MissingPermission") // verificada em podeNotificar()
    fun notificar(alertas: List<Alerta>) {
        if (alertas.isEmpty() || !podeNotificar()) return

        val linhas = alertas.map(::textoDoAlerta)
        val titulo = if (alertas.size == 1) "Manutenção necessária" else "${alertas.size} itens precisam de atenção"
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacao = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(linhas.first())
            .setStyle(NotificationCompat.InboxStyle().also { estilo -> linhas.forEach(estilo::addLine) })
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICACAO_ID, notificacao)
    }

    private fun podeNotificar(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun textoDoAlerta(alerta: Alerta): String {
        val situacao = when (alerta.origem to alerta.status) {
            OrigemAlerta.PECA to StatusDesgaste.TROCAR -> "troca necessária"
            OrigemAlerta.SERVICO to StatusDesgaste.TROCAR -> "serviço vencido"
            else -> "${(alerta.fracao * 100).roundToInt()}% do limite"
        }
        return "${alerta.item} (${alerta.bikeNome}): $situacao"
    }

    private companion object {
        const val CANAL_ID = "alertas_manutencao"
        const val NOTIFICACAO_ID = 1
    }
}
