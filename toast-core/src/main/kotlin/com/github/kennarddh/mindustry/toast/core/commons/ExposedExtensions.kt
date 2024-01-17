package com.github.kennarddh.mindustry.toast.core.commons

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

fun FieldSet.exists(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean = !selectAll().where(where).empty()
fun FieldSet.selectOne(where: SqlExpressionBuilder.() -> Op<Boolean>): ResultRow? =
    selectAll().where(where).firstOrNull()

fun Table.mediumblob(name: String): Column<ExposedBlob> =
    registerColumn(name, MediumBlobColumnType())

private class MediumBlobColumnType : IColumnType by BlobColumnType() {
    override fun sqlType(): String = "MEDIUMBLOB"
}
