package br.com.viavalesistemas.manutencaobike.model

data class Bike(
    val id: Long = 0,
    val nome: String,
    val marca: String? = null,
    val modelo: String? = null,
    val kmAtual: Double,
    val stravaGearId: String? = null,
    val arquivada: Boolean = false,
) {
    val doStrava: Boolean get() = stravaGearId != null

    private fun String.normalizado() = trim().replace(Regex("\\s+"), " ")
    /** Marca e modelo, omitido quando só repete o nome (comum em bikes do Strava). */
    val descricao: String?
        get() = listOfNotNull(marca, modelo).joinToString(" ").normalizado().ifBlank { null }
            ?.takeUnless { it.equals(nome.normalizado(), ignoreCase = true) }
}
