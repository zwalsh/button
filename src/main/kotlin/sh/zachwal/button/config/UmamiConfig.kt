package sh.zachwal.button.config

data class UmamiConfig(
    val umamiUrl: String,
    val websiteId: String,
)

// Initialize this global variable so it is accessible in HTML utilities
lateinit var umamiConfig: UmamiConfig

fun initUmami(config: UmamiConfig) {
    umamiConfig = config
}
