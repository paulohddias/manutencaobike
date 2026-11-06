package br.com.viavalesistemas.manutencaobike.ui

import br.com.viavalesistemas.manutencaobike.data.DataError
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val localeBr = Locale.forLanguageTag("pt-BR")
private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy", localeBr)

fun formatarKm(km: Double): String = "${NumberFormat.getIntegerInstance(localeBr).format(km.roundToLong())} km"

fun formatarMoeda(centavos: Long): String = NumberFormat.getCurrencyInstance(localeBr).format(centavos / 100.0)

fun formatarData(data: LocalDate): String = data.format(formatoData)

fun formatarPercentual(fracao: Double): String = "${(fracao * 100).roundToInt()}%"

/** Aceita "3000" ou "3.000"; devolve null se não houver dígitos. */
fun lerInteiro(texto: String): Int? = texto.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.toIntOrNull()

/** Aceita "45,90" ou "45.90". */
fun lerCentavos(texto: String): Long? = texto.trim().replace(',', '.').toBigDecimalOrNull()
    ?.takeIf { it.signum() >= 0 }
    ?.setScale(2, RoundingMode.HALF_UP)
    ?.movePointRight(2)
    ?.toLong()

fun centavosParaTexto(centavos: Long?): String =
    centavos?.let { "%d,%02d".format(it / 100, it % 100) }.orEmpty()

fun Throwable.mensagemParaUsuario(): String =
    (this as? DataError)?.message ?: "Algo deu errado. Tente novamente."
