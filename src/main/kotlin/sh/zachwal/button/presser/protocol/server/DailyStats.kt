package sh.zachwal.button.presser.protocol.server

data class DailyStats(
    val uniquePressers: Int,
    val peakConcurrent: Int,
    val totalPresses: Int,
) : ServerMessage
