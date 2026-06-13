package com.worklog.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Duty : Screen("duty", "值班", Icons.Default.CalendarMonth)
    data object Attendance : Screen("attendance", "考勤", Icons.Default.Checklist)
    data object Booking : Screen("booking", "预约", Icons.Default.Event)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)

    companion object {
        val items = listOf(Home, Duty, Attendance, Booking, Stats, Settings)
    }
}
