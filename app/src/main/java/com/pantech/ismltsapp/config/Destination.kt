package com.pantech.ismltsapp.config

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.pantech.ismltsapp.R

/**
 * The website pages the app's tiles and bottom bar open: one place to change a path or a title.
 * [navItem] is the bottom-bar item to highlight, or null when the page has no item there.
 */
enum class Destination(val path: String, @get:StringRes val title: Int, @get:IdRes val navItem: Int?) {
    ANNOUNCEMENTS("Announcements", R.string.nav_messages, R.id.nav_messages),
    PROGRESS("Insights/MyProgress", R.string.nav_favourites, R.id.nav_favourites),
    CALENDAR("Calendar", R.string.nav_menu, R.id.nav_menu),
    PROFILE("Profile", R.string.nav_profile, null),
    SCAN_IN("Attendance/Scan", R.string.nav_scan, null),
    WORK_TO_DO("Assessments/MyAssessments", R.string.nav_work, null);

    companion object {
        fun forNavItem(@IdRes id: Int): Destination? = entries.firstOrNull { it.navItem == id }
    }
}
