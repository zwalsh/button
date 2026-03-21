package sh.zachwal.button.presser.protocol.server

data class Snapshot(
    val count: Int,
    val names: List<String>,
    val dailyStats: DailyStats,
) : ServerMessage
