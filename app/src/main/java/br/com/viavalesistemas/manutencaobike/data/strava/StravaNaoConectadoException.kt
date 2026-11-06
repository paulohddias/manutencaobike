package br.com.viavalesistemas.manutencaobike.data.strava

import java.io.IOException

// Precisa ser IOException: é lançada de dentro de um Interceptor do OkHttp,
// e qualquer outro tipo derrubaria o app na thread do OkHttp.
class StravaNaoConectadoException(cause: Throwable? = null) :
    IOException("Conta do Strava não conectada", cause)
