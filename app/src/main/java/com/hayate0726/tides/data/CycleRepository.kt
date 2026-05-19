package com.hayate0726.tides.data

import com.hayate0726.tides.data.dao.BirthControlDao
import com.hayate0726.tides.data.dao.CycleEntryDao
import com.hayate0726.tides.data.dao.GoalDao
import com.hayate0726.tides.data.dao.SymptomEntryDao
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import java.time.LocalDate

/**
 * Bridges Room DAOs to domain types. UI code uses this; it does not
 * touch DAOs directly.
 */
class CycleRepository(
    private val cycleEntryDao: CycleEntryDao,
    private val symptomEntryDao: SymptomEntryDao,
    private val birthControlDao: BirthControlDao,
    private val goalDao: GoalDao,
) {

    suspend fun detectCycles(from: LocalDate, to: LocalDate): List<Cycle> {
        val entries = cycleEntryDao.rangeOnce(from, to)
        val activeBc = birthControlDao.activeOnce()?.method
        return CycleDetector.detect(
            entries.map { CycleDetector.Entry(it.date, it.flowIntensity) },
            activeBirthControl = activeBc,
        )
    }

    suspend fun symptomEntriesInRange(from: LocalDate, to: LocalDate): List<SymptomEntryEntity> =
        symptomEntryDao.rangeOnce(from, to)

    suspend fun activeBirthControl(): BirthControlEntity? = birthControlDao.activeOnce()

    suspend fun goals(): Set<Goal> = goalDao.all().toSet()
}
