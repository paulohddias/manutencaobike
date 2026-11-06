package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

/** v2: "arquivada" vira "inativa" (controlada pelo usuário) e a bike ganha foto. */
@RenameColumn(tableName = "bikes", fromColumnName = "arquivada", toColumnName = "inativa")
class Migracao1Para2 : AutoMigrationSpec
