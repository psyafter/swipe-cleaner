package com.swipecleaner

import android.content.Context

class MonetizationStore(context: Context) {
    private val prefs = context.getSharedPreferences("swipe_cleaner_prefs", Context.MODE_PRIVATE)

    fun getFreeDeleteUsedCount(): Int = prefs.getInt(KEY_FREE_DELETE_USED_COUNT, 0)

    fun setFreeDeleteUsedCount(value: Int) {
        prefs.edit().putInt(KEY_FREE_DELETE_USED_COUNT, value).apply()
    }

    fun getIsProUnlocked(): Boolean = prefs.getBoolean(KEY_IS_PRO_UNLOCKED, false)

    fun setIsProUnlocked(value: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO_UNLOCKED, value).apply()
    }

    fun getHasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun setHasSeenOnboarding(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, value).apply()
    }

    fun getRequireDeleteConfirmation(): Boolean = prefs.getBoolean(KEY_REQUIRE_DELETE_CONFIRMATION, true)

    fun setRequireDeleteConfirmation(value: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_DELETE_CONFIRMATION, value).apply()
    }

    private companion object {
        const val KEY_FREE_DELETE_USED_COUNT = "free_delete_used_count"
        const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"
        const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_REQUIRE_DELETE_CONFIRMATION = "require_delete_confirmation"
    }
}
