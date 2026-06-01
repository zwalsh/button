package sh.zachwal.button.db.jdbi

data class NotificationPreferences(
    // TODO: Default to false once the opt-in onboarding flow is built — see PhoneBookService.register()
    val notificationsEnabled: Boolean,
)
