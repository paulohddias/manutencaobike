package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {
    @Query("SELECT * FROM bikes ORDER BY inativa, nome COLLATE NOCASE")
    fun observarTodas(): Flow<List<BikeEntity>>

    @Query("SELECT * FROM bikes WHERE id = :id")
    fun observar(id: Long): Flow<BikeEntity?>

    @Query("SELECT * FROM bikes WHERE id = :id")
    suspend fun buscar(id: Long): BikeEntity?

    @Query("SELECT * FROM bikes WHERE inativa = 0")
    suspend fun listarAtivas(): List<BikeEntity>

    @Query("SELECT * FROM bikes WHERE stravaGearId = :gearId")
    suspend fun buscarPorStrava(gearId: String): BikeEntity?

    @Insert
    suspend fun inserir(bike: BikeEntity): Long

    @Update
    suspend fun atualizar(bike: BikeEntity)

    @Query("UPDATE bikes SET kmAtual = :km WHERE id = :id")
    suspend fun atualizarKm(id: Long, km: Double)

    @Query("UPDATE bikes SET inativa = :inativa WHERE id = :id")
    suspend fun definirInativa(id: Long, inativa: Boolean)

    @Query("DELETE FROM bikes WHERE id = :id")
    suspend fun excluir(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ignorarStrava(ignorada: StravaIgnoradaEntity)

    @Query("SELECT gearId FROM strava_ignoradas")
    suspend fun gearsIgnorados(): List<String>

    @Query("DELETE FROM strava_ignoradas")
    suspend fun limparIgnorados()

    // Peças e serviços saem junto pelo CASCADE das chaves estrangeiras.
    @Transaction
    suspend fun excluirDefinitivo(bike: BikeEntity) {
        bike.stravaGearId?.let { ignorarStrava(StravaIgnoradaEntity(it)) }
        excluir(bike.id)
    }
}
