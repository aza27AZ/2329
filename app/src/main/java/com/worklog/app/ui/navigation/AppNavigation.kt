package com.worklog.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.worklog.app.data.AppDatabase
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.ui.screens.*
import com.worklog.app.ui.theme.Blue
import com.worklog.app.ui.theme.Gray
import com.worklog.app.ui.theme.GrayBackground
import com.worklog.app.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val repository = remember { DataRepository(AppDatabase.getInstance(context)) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = White,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            unselectedIconColor = Gray,
                            unselectedTextColor = Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = GrayBackground
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GrayBackground)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToTab = { index ->
                    if (index >= 0 && index < Screen.items.size) {
                        navController.navigate(Screen.items[index].route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
            }
            composable(Screen.Duty.route) {
                DutyScreen(repository)
            }
            composable(Screen.Attendance.route) {
                AttendanceScreen(repository)
            }
            composable(Screen.Booking.route) {
                BookingScreen(repository)
            }
            composable(Screen.Stats.route) {
                StatsScreen(repository)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(repository)
            }
        }
    }
}
