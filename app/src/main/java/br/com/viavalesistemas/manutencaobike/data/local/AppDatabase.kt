package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BikeEntity::class,
        TipoPecaEntity::class,
        TipoServicoEntity::class,
        PecaEntity::class,
        ServicoEntity::class,
        StravaIgnoradaEntity::class,
    ],
    version = AppDatabase.VERSAO,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = Migracao1Para2::class),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun pecaDao(): PecaDao
    abstract fun servicoDao(): ServicoDao

    companion object {
        const val NOME = "manutencao_bike.db"
        const val VERSAO = 3
    }
}
