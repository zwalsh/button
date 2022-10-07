package sh.zachwal.button.presshistory

import com.google.inject.Inject
import com.google.inject.Singleton
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant

@Singleton
class PressHistoryService @Inject constructor(private val pressDAO: PressDAO) {

    fun createPress(ip: String) {
        pressDAO.createPress(ip)
    }

    fun listPresses(since: Instant): List<Press> {
        return pressDAO.selectSince(since)
    }

    fun countPresses(since: Instant = Instant.EPOCH): Long {
        return pressDAO.countSince(since)
    }
}
