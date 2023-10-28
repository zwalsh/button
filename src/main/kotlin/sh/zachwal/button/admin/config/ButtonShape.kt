package sh.zachwal.button.admin.config

import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.CUBE
import sh.zachwal.button.admin.config.ButtonShape.FIREWORKS

enum class ButtonShape {
    CIRCLE,
    CUBE,
    SHAMROCK,
    HEART,
    CHRISTMAS_TREE,
    TURKEY,
    PUMPKIN,
    FIREWORKS,
}

fun ButtonShape.isSpecial(): Boolean {
    return this !in setOf(CIRCLE, CUBE, FIREWORKS)
}
