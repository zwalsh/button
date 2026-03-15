package sh.zachwal.button.db.jdbi

import java.time.LocalDate

data class DailyStatsRow(
    val date: LocalDate,
    val peakConcurrent: Int,
    val totalPressCount: Int,
)
