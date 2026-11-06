package br.com.viavalesistemas.manutencaobike.data.local

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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun pecaDao(): PecaDao
    abstract fun servicoDao(): ServicoDao
}
