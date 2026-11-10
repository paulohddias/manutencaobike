package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Bikes do Strava que o usuário excluiu: sem esse registro a sincronização importaria de novo.
// Fica no banco (e não no DataStore) para ir junto no backup.
@Entity(tableName = "strava_ignoradas")
data class StravaIgnoradaEntity(@PrimaryKey val gearId: String)
