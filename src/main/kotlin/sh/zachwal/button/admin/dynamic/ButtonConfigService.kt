package sh.zachwal.button.admin.dynamic

import sh.zachwal.button.config.AppConfig
import javax.inject.Inject

class ButtonConfigService @Inject constructor(config: AppConfig) {

    private var isCube = config.cubeButton

    fun isCube(): Boolean = isCube

    fun setCube(cube: Boolean) {
        isCube = cube
    }
}
