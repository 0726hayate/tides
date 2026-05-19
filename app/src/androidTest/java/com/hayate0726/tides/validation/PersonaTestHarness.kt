package com.hayate0726.tides.validation

import android.content.Context
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.entity.GoalEntity
import java.io.File
import java.util.UUID

object PersonaTestHarness {

    /** Static AES-256 key derived once per process — sufficient for tests. */
    private val testKey: DbKey by lazy { DbKey(ByteArray(32) { (it * 7 + 1).toByte() }) }

    /**
     * Open a fresh ephemeral DB pre-populated with [persona] data. Caller
     * is responsible for calling [close] which closes the DB and deletes
     * the file.
     */
    suspend fun openWithPersona(ctx: Context, persona: Persona): TidesDatabase {
        val file = File(ctx.filesDir, "persona-${persona.id}-${UUID.randomUUID()}.db")
        if (file.exists()) file.delete()
        val db = DatabaseFactory.open(ctx, file, testKey)
        persona.cycleEntries.forEach { db.cycleEntryDao().upsert(it) }
        persona.symptomEntries.forEach { db.symptomEntryDao().insert(it) }
        persona.goals.forEach { db.goalDao().insert(GoalEntity(it)) }
        persona.birthControl?.let { db.birthControlDao().insert(it) }
        return db
    }

    fun close(ctx: Context, db: TidesDatabase, persona: Persona) {
        db.close()
        ctx.filesDir.listFiles { f -> f.name.startsWith("persona-${persona.id}-") }
            ?.forEach { it.delete() }
    }
}
