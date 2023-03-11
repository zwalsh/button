package sh.zachwal.button.admin.config

import org.slf4j.LoggerFactory
import sh.zachwal.button.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ButtonConfigService @Inject constructor(config: AppConfig) {

    private val logger = LoggerFactory.getLogger(ButtonConfigService::class.java)

    private var isCube = config.cubeButton

    fun isCube(): Boolean = isCube

    fun setCube(cube: Boolean) {
        isCube = cube
        logger.info("Set cube to $isCube")
    }
}
