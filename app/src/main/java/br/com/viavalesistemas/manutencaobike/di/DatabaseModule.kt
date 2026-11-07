package br.com.viavalesistemas.manutencaobike.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import br.com.viavalesistemas.manutencaobike.data.backup.PacoteBackup
import br.com.viavalesistemas.manutencaobike.data.local.AppDatabase
import br.com.viavalesistemas.manutencaobike.data.local.CatalogoInicial
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase {
        // Uma restauração de backup só pode trocar os arquivos antes de o Room abrir o banco.
        PacoteBackup.aplicarRestauracaoPendente(context)
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOME)
            .addCallback(CatalogoInicial)
            .build()
    }

    @Provides
    fun bikeDao(db: AppDatabase) = db.bikeDao()

    @Provides
    fun catalogoDao(db: AppDatabase) = db.catalogoDao()

    @Provides
    fun pecaDao(db: AppDatabase) = db.pecaDao()

    @Provides
    fun servicoDao(db: AppDatabase) = db.servicoDao()

    @Provides
    @Singleton
    fun preferencias(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("preferencias") }
}
