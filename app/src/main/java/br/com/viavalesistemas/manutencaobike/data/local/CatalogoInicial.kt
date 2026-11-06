package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Popula o catálogo na primeira abertura; o usuário pode editar tudo depois. */
object CatalogoInicial : RoomDatabase.Callback() {

    private val tiposPeca = listOf(
        "Corrente" to 3_000,
        "Cassete" to 9_000,
        "Coroa(s) do pedivela" to 15_000,
        "Pneu dianteiro" to 5_000,
        "Pneu traseiro" to 3_500,
        "Pastilha de freio dianteira" to 1_500,
        "Pastilha de freio traseira" to 1_500,
        "Disco de freio" to 10_000,
        "Cabos e conduítes" to 5_000,
        "Fita de guidão" to 5_000,
        "Movimento central" to 10_000,
    )

    private val tiposServico = listOf(
        Triple("Revisão geral", 2_000, 6),
        Triple("Lubrificação da corrente", 300, null),
        Triple("Sangria de freio hidráulico", null, 12),
        Triple("Revisão de suspensão", null, 12),
        Triple("Troca de selante tubeless", null, 3),
        Triple("Regulagem de câmbio", 1_000, null),
    )

    override fun onCreate(db: SupportSQLiteDatabase) {
        tiposPeca.forEach { (nome, vidaUtil) ->
            db.execSQL(
                "INSERT INTO tipos_peca (nome, vidaUtilKmPadrao) VALUES (?, ?)",
                arrayOf<Any?>(nome, vidaUtil),
            )
        }
        tiposServico.forEach { (nome, km, meses) ->
            db.execSQL(
                "INSERT INTO tipos_servico (nome, intervaloKm, intervaloMeses) VALUES (?, ?, ?)",
                arrayOf<Any?>(nome, km, meses),
            )
        }
    }
}
