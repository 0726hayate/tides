package com.hayate0726.tides.data.`import`

import com.hayate0726.tides.domain.model.FlowIntensity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.InputStream
import java.time.LocalDate
import java.time.Month

class SamsungHealthHtmlImporter : ImportSource {

    override val displayName: String = "Samsung Health"

    companion object {
        private const val MIN_YEAR = 1900

        private val MONTH_MAP: Map<String, Month> = mapOf(
            "jan" to Month.JANUARY, "feb" to Month.FEBRUARY, "mar" to Month.MARCH,
            "apr" to Month.APRIL, "may" to Month.MAY, "jun" to Month.JUNE,
            "jul" to Month.JULY, "aug" to Month.AUGUST, "sep" to Month.SEPTEMBER,
            "oct" to Month.OCTOBER, "nov" to Month.NOVEMBER, "dec" to Month.DECEMBER,
        )

        // Matches patterns like:
        //   "Apr 8–10"          (same-month range, en-dash)
        //   "Apr 8–May 3"       (cross-month range, en-dash)
        //   "Apr 8-10"          (same-month range, hyphen-minus fallback)
        //   "Apr 8-May 3"       (cross-month range, hyphen-minus fallback)
        // Group 1: start month, Group 2: start day,
        // Group 3: optional end month, Group 4: end day
        private val DATE_RANGE_REGEX = Regex(
            """(?i)(${MONTH_MAP.keys.joinToString("|")})\s+(\d{1,2})\s*[–\-]\s*(?:(${MONTH_MAP.keys.joinToString("|")})\s+)?(\d{1,2})""",
        )
    }

    override suspend fun parse(stream: InputStream): ParseResult {
        val doc: Document = Jsoup.parse(stream, Charsets.UTF_8.name(), "")

        val warnings = mutableListOf<String>()
        val today = LocalDate.now()

        // Infer the document year. Samsung's HTML export doesn't embed an explicit
        // 4-digit year, so we look for one in the document text and fall back to
        // today's year.
        val docYear = Regex("""\b(20\d{2})\b""").find(doc.text())?.groupValues?.get(1)?.toInt()
            ?: today.year

        // Find all tables in the document and look for one that has a "Period"
        // column header. Samsung's export uses a flat <td> for headers (bold +
        // background colour), not <th>. We identify the header row as the first
        // <tr> whose cells contain "Period" as one of the cell texts.
        val entries = mutableListOf<ImportedEntry>()

        for (table in doc.select("table")) {
            val rows = table.select("tr")
            if (rows.isEmpty()) continue

            // Scan for a header row that contains a cell whose text equals "Period".
            var periodColIdx = -1
            var dataStartIdx = -1
            for ((rowIdx, row) in rows.withIndex()) {
                val cells = row.select("td, th")
                val periodIdx = cells.indexOfFirst { it.text().trim().equals("Period", ignoreCase = true) }
                if (periodIdx >= 0) {
                    periodColIdx = periodIdx
                    dataStartIdx = rowIdx + 1
                    break
                }
            }

            if (periodColIdx < 0) continue // This table has no "Period" header

            // Extract date ranges from the Period column of each data row.
            for (rowIdx in dataStartIdx until rows.size) {
                val cells = rows[rowIdx].select("td, th")
                if (cells.size <= periodColIdx) continue

                val cellText = cells[periodColIdx].text().trim()
                if (cellText.isBlank()) continue

                val match = DATE_RANGE_REGEX.find(cellText) ?: continue

                val startMonth = MONTH_MAP[match.groupValues[1].lowercase()] ?: continue
                val startDay = match.groupValues[2].toIntOrNull() ?: continue
                val endMonthName = match.groupValues[3].takeIf { it.isNotEmpty() }
                val endMonth = endMonthName?.let { MONTH_MAP[it.lowercase()] } ?: startMonth
                val endDay = match.groupValues[4].toIntOrNull() ?: continue

                val startDate = try {
                    LocalDate.of(docYear, startMonth, startDay)
                } catch (e: Exception) {
                    warnings += "Couldn't build start date from '$cellText'"
                    continue
                }

                // If the end month is earlier in the year than the start month
                // (e.g. Dec–Jan), the end date is in the following year.
                val endYear = if (endMonth.value < startMonth.value) docYear + 1 else docYear
                val endDate = try {
                    LocalDate.of(endYear, endMonth, endDay)
                } catch (e: Exception) {
                    warnings += "Couldn't build end date from '$cellText'"
                    continue
                }

                if (endDate.isBefore(startDate)) {
                    warnings += "End date before start date in '$cellText', skipping"
                    continue
                }

                // Expand the range into per-day entries, applying the date-range guard.
                var d = startDate
                while (!d.isAfter(endDate)) {
                    if (d.year < MIN_YEAR) {
                        warnings += "Date before $MIN_YEAR, skipping: $d"
                    } else if (d.isAfter(today)) {
                        warnings += "Future date, skipping: $d"
                    } else {
                        entries += ImportedEntry(
                            date = d,
                            // Samsung's "Share data" HTML export reports period date ranges
                            // but doesn't break down flow intensity per day in this view.
                            // We record LIGHT as a baseline (period confirmed present).
                            flow = FlowIntensity.LIGHT,
                            painSeverity = null,
                            symptoms = emptyList(),
                            notes = null,
                        )
                    }
                    d = d.plusDays(1)
                }
            }
        }

        // Deduplicate by date (in case multiple tables or rows list the same day),
        // keep the first occurrence, then sort ascending.
        val deduped = entries
            .groupBy { it.date }
            .map { (_, v) -> v.first() }
            .sortedBy { it.date }

        return ParseResult(deduped, emptyList(), warnings)
    }
}
