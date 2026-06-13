package com.worklog.app.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worklog.app.data.AppDatabase
import com.worklog.app.data.entity.*
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.ui.theme.*
import com.worklog.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

internal data class RecentRecordItem(
    val id: Long,
    val type: String,
    val date: String,
    val summary: String,
    val employeeName: String? = null
)

internal class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataRepository(AppDatabase.getInstance(application))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResultEmployee = MutableStateFlow<Employee?>(null)
    val searchResultEmployee: StateFlow<Employee?> = _searchResultEmployee.asStateFlow()

    private val _employeeSummary = MutableStateFlow<EmployeeSummaryState?>(null)
    val employeeSummary: StateFlow<EmployeeSummaryState?> = _employeeSummary.asStateFlow()

    private val _todayDuties = MutableStateFlow<List<DutyWithEmployee>>(emptyList())
    val todayDuties: StateFlow<List<DutyWithEmployee>> = _todayDuties.asStateFlow()

    private val _upcomingBookings = MutableStateFlow<List<BookingWithVenue>>(emptyList())
    val upcomingBookings: StateFlow<List<BookingWithVenue>> = _upcomingBookings.asStateFlow()

    private val _operationDays = MutableStateFlow(0)
    val operationDays: StateFlow<Int> = _operationDays.asStateFlow()

    private val _totalInbound = MutableStateFlow(0)
    val totalInbound: StateFlow<Int> = _totalInbound.asStateFlow()

    private val _quarterlyBookingCount = MutableStateFlow(0)
    val quarterlyBookingCount: StateFlow<Int> = _quarterlyBookingCount.asStateFlow()

    private val _recentRecords = MutableStateFlow<List<RecentRecordItem>>(emptyList())
    val recentRecords: StateFlow<List<RecentRecordItem>> = _recentRecords.asStateFlow()

    val showSummary: StateFlow<Boolean> = _searchResultEmployee.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _allEmployees = MutableStateFlow<List<Employee>>(emptyList())
    private val _allVenues = MutableStateFlow<List<Venue>>(emptyList())
    private val _allAttendanceTypes = MutableStateFlow<List<AttendanceType>>(emptyList())

    private val today = DateUtils.today()
    private val currentYearMonth = DateUtils.currentYearMonth()

    init {
        loadEmployees()
        loadVenues()
        loadAttendanceTypes()
        loadTodayDuties()
        loadUpcomingBookings()
        loadVenueDashboard()
        loadRecentRecords()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            clearSearch()
            return
        }
        viewModelScope.launch {
            val found = _allEmployees.value.find {
                it.isActive && it.name.contains(query, ignoreCase = true)
            }
            _searchResultEmployee.value = found
            if (found != null) {
                loadEmployeeSummary(found)
            } else {
                _employeeSummary.value = null
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResultEmployee.value = null
        _employeeSummary.value = null
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            repository.getActiveEmployees().collect { emps ->
                _allEmployees.value = emps
            }
        }
    }

    private fun loadVenues() {
        viewModelScope.launch {
            repository.getAllVenues().collect { venues ->
                _allVenues.value = venues
            }
        }
    }

    private fun loadAttendanceTypes() {
        viewModelScope.launch {
            repository.getAllAttendanceTypes().collect { types ->
                _allAttendanceTypes.value = types
            }
        }
    }

    private fun loadEmployeeSummary(employee: Employee) {
        viewModelScope.launch {
            val start = DateUtils.getMonthStart(currentYearMonth)
            val end = DateUtils.getMonthEnd(currentYearMonth)

            val shiftCounts = repository.getShiftCounts(employee.id, start, end)
            val earlyCount = shiftCounts.find { it.shiftType == "早班" }?.count ?: 0
            val lateCount = shiftCounts.find { it.shiftType == "晚班" }?.count ?: 0
            val fullCount = shiftCounts.find { it.shiftType == "通班" }?.count ?: 0
            val totalDuty = earlyCount + lateCount + fullCount

            val attendanceRecords = repository.getAttendanceByDateRange(start, end).first()
            val attendanceCount = attendanceRecords.count { it.employeeId == employee.id }

            val allBookings = repository.getAllBookings().first()
            val bookingCount = allBookings.count { it.contactId == employee.id && it.date >= start && it.date <= end }

            _employeeSummary.value = EmployeeSummaryState(
                name = employee.name,
                totalDuty = totalDuty,
                attendanceCount = attendanceCount,
                bookingCount = bookingCount
            )
        }
    }

    private fun loadTodayDuties() {
        viewModelScope.launch {
            val duties = repository.getDutiesByDate(today)
            val employees = repository.getActiveEmployees().first()
            _todayDuties.value = duties.map { duty ->
                DutyWithEmployee(
                    duty = duty,
                    employeeName = employees.find { it.id == duty.employeeId }?.name ?: "未知"
                )
            }
        }
    }

    private fun loadUpcomingBookings() {
        viewModelScope.launch {
            combine(
                repository.getAllBookings(),
                repository.getAllVenues()
            ) { bookings, venues ->
                val todayDate = LocalDate.now()
                val endDate = todayDate.plusDays(3)
                bookings
                    .filter { b ->
                        val bd = DateUtils.parseDate(b.date)
                        bd != null && !bd.isBefore(todayDate) && !bd.isAfter(endDate) && b.status != "已取消"
                    }
                    .sortedBy { b -> b.date }
                    .map { b ->
                        BookingWithVenue(
                            booking = b,
                            venueName = venues.find { it.id == b.venueId }?.name ?: "未知场馆"
                        )
                    }
            }.collect { result ->
                _upcomingBookings.value = result
            }
        }
    }

    private fun loadVenueDashboard() {
        viewModelScope.launch {
            _operationDays.value = repository.getOperationDays()
            _totalInbound.value = repository.getTotalInboundCount()

            val now = LocalDate.now()
            val currentQuarter = (now.monthValue - 1) / 3 + 1
            val quarterStart = LocalDate.of(now.year, (currentQuarter - 1) * 3 + 1, 1)
            val quarterEnd = quarterStart.plusMonths(3).minusDays(1)

            val bookings = repository.getAllBookings().first()
            _quarterlyBookingCount.value = bookings.count { b ->
                val bd = DateUtils.parseDate(b.date)
                bd != null && !bd.isBefore(quarterStart) && !bd.isAfter(quarterEnd) && b.status != "已取消"
            }
        }
    }

    fun saveInboundRecord(yearMonth: String, count: Int) {
        viewModelScope.launch {
            repository.saveInboundRecord(InboundRecord(yearMonth = yearMonth, count = count))
            _operationDays.value = repository.getOperationDays()
            _totalInbound.value = repository.getTotalInboundCount()
        }
    }

    private fun loadRecentRecords() {
        viewModelScope.launch {
            val monthStart = DateUtils.getMonthStart(currentYearMonth)
            val monthEnd = DateUtils.getMonthEnd(currentYearMonth)

            combine(
                combine(
                    repository.getDutiesByRange(monthStart, monthEnd),
                    repository.getAttendanceByDateRange(monthStart, monthEnd),
                    repository.getAllBookings()
                ) { duties, attendances, bookings ->
                    Triple(duties, attendances, bookings)
                },
                combine(
                    repository.getActiveEmployees(),
                    repository.getAllVenues(),
                    repository.getAllAttendanceTypes()
                ) { employees, venues, types ->
                    Triple(employees, venues, types)
                }
            ) { triple1, triple2 ->
                val (duties, attendances, bookings) = triple1
                val (employees, venues, types) = triple2
                val items = mutableListOf<RecentRecordItem>()

                duties.forEach { d ->
                    val name = employees.find { it.id == d.employeeId }?.name ?: "未知"
                    items.add(
                        RecentRecordItem(
                            id = d.id,
                            type = "duty",
                            date = d.date,
                            summary = "${name} ${d.shiftType}",
                            employeeName = name
                        )
                    )
                }

                attendances.forEach { a ->
                    val name = employees.find { it.id == a.employeeId }?.name ?: "未知"
                    val typeName = types.find { it.id == a.typeId }?.name ?: "考勤"
                    items.add(
                        RecentRecordItem(
                            id = a.id,
                            type = "attendance",
                            date = a.startDate,
                            summary = "${name} ${typeName}",
                            employeeName = name
                        )
                    )
                }

                val todayDate = LocalDate.now()
                val futureLimit = todayDate.plusDays(30)
                bookings
                    .filter { b ->
                        val bd = DateUtils.parseDate(b.date)
                        bd != null && !bd.isAfter(futureLimit) && b.status != "已取消"
                    }
                    .forEach { b ->
                        val venueName = venues.find { it.id == b.venueId }?.name ?: "未知"
                        items.add(
                            RecentRecordItem(
                                id = b.id,
                                type = "booking",
                                date = b.date,
                                summary = "${b.department} @ ${venueName}",
                                employeeName = b.department
                            )
                        )
                    }

                items.sortedByDescending { it.date + it.type + it.id }
            }.collect { items ->
                _recentRecords.value = items
            }
        }
    }
}

internal data class DutyWithEmployee(
    val duty: DutyRecord,
    val employeeName: String
)

internal data class BookingWithVenue(
    val booking: BookingRecord,
    val venueName: String
)

internal data class EmployeeSummaryState(
    val name: String,
    val totalDuty: Int,
    val attendanceCount: Int,
    val bookingCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onNavigateToTab: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val employeeSummary by viewModel.employeeSummary.collectAsState()
    val showSummary by viewModel.showSummary.collectAsState()
    val todayDuties by viewModel.todayDuties.collectAsState()
    val upcomingBookings by viewModel.upcomingBookings.collectAsState()
    val operationDays by viewModel.operationDays.collectAsState()
    val totalInbound by viewModel.totalInbound.collectAsState()
    val quarterlyBookingCount by viewModel.quarterlyBookingCount.collectAsState()
    val recentRecords by viewModel.recentRecords.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch() },
                onClear = { viewModel.clearSearch() }
            )
        }

        if (showSummary && employeeSummary != null) {
            item {
                EmployeeSummaryCard(summary = employeeSummary!!)
            }
        }

        item {
            TodayDutyCard(duties = todayDuties)
        }

        item {
            QuickEntrySection(onNavigateToTab = onNavigateToTab)
        }

        item {
            VenueDashboardCard(
                operationDays = operationDays,
                totalInbound = totalInbound,
                quarterlyBookingCount = quarterlyBookingCount,
                onSaveInbound = { yearMonth, count -> viewModel.saveInboundRecord(yearMonth, count) }
            )
        }

        if (upcomingBookings.isNotEmpty()) {
            item {
                UpcomingBookingsSection(bookings = upcomingBookings)
            }
        }

        item {
            RecentRecordsSection(records = recentRecords)
        }
    }
}

@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "工作量化管理",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索员工姓名...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun EmployeeSummaryCard(summary: EmployeeSummaryState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${DateUtils.currentYearMonth()} 汇总",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "值班次数",
                    value = "${summary.totalDuty}",
                    color = Yellow
                )
                StatItem(
                    label = "考勤事件",
                    value = "${summary.attendanceCount}",
                    color = Red
                )
                StatItem(
                    label = "对接预约",
                    value = "${summary.bookingCount}",
                    color = Green
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayDutyCard(duties: List<DutyWithEmployee>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "今日值班",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateUtils.formatDisplayFull(DateUtils.today()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (duties.isEmpty()) {
                Text(
                    text = "今日暂无值班安排",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val earlyDuties = duties.filter { it.duty.shiftType == "早班" }
                val lateDuties = duties.filter { it.duty.shiftType == "晚班" }
                val fullDuties = duties.filter { it.duty.shiftType == "通班" }

                DutyShiftRow(shiftLabel = "早班", employees = earlyDuties.map { it.employeeName }, shiftColor = Yellow)
                Spacer(modifier = Modifier.height(8.dp))
                DutyShiftRow(shiftLabel = "晚班", employees = lateDuties.map { it.employeeName }, shiftColor = Blue)
                Spacer(modifier = Modifier.height(8.dp))
                DutyShiftRow(shiftLabel = "通班", employees = fullDuties.map { it.employeeName }, shiftColor = Purple)
            }
        }
    }
}

@Composable
private fun DutyShiftRow(shiftLabel: String, employees: List<String>, shiftColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shiftColor.copy(alpha = 0.15f))
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shiftLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = shiftColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (employees.isEmpty()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = employees.joinToString("、"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickEntrySection(onNavigateToTab: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "快捷录入",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickEntryButton(
                label = "添加值班",
                icon = Icons.Default.AddCircleOutline,
                color = Yellow,
                onClick = { onNavigateToTab(1) },
                modifier = Modifier.weight(1f)
            )
            QuickEntryButton(
                label = "考勤登记",
                icon = Icons.Default.Checklist,
                color = Red,
                onClick = { onNavigateToTab(2) },
                modifier = Modifier.weight(1f)
            )
            QuickEntryButton(
                label = "新建预约",
                icon = Icons.Default.Event,
                color = Green,
                onClick = { onNavigateToTab(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickEntryButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VenueDashboardCard(
    operationDays: Int,
    totalInbound: Int,
    quarterlyBookingCount: Int,
    onSaveInbound: (String, Int) -> Unit
) {
    var showInboundDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "场馆运营看板",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashboardStatItem(
                    label = "运营天数",
                    value = "${operationDays}",
                    icon = Icons.Default.DateRange,
                    color = Blue
                )
                DashboardStatItem(
                    label = "总入馆人数",
                    value = "${totalInbound}",
                    icon = Icons.Default.People,
                    color = Green
                )
                DashboardStatItem(
                    label = "本季预约数",
                    value = "${quarterlyBookingCount}",
                    icon = Icons.Default.EventAvailable,
                    color = Orange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showInboundDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("录入本月入馆人数")
            }
        }
    }

    if (showInboundDialog) {
        InboundInputDialog(
            currentYearMonth = DateUtils.currentYearMonth(),
            onDismiss = { showInboundDialog = false },
            onSave = { yearMonth, count ->
                onSaveInbound(yearMonth, count)
                showInboundDialog = false
            }
        )
    }
}

@Composable
private fun DashboardStatItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InboundInputDialog(
    currentYearMonth: String,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var countInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "录入入馆人数",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "月份：${currentYearMonth}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { countInput = it.filter { c -> c.isDigit() } },
                    label = { Text("入馆人数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(currentYearMonth, countInput.toIntOrNull() ?: 0) },
                enabled = countInput.isNotEmpty()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun UpcomingBookingsSection(bookings: List<BookingWithVenue>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "即将到来预约",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "未来3天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            bookings.forEach { item ->
                BookingRow(item = item)
                if (item != bookings.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingRow(item: BookingWithVenue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Green.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.booking.department,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${DateUtils.formatDisplay(item.booking.date)} ${item.booking.startTime}-${item.booking.endTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = item.venueName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentRecordsSection(records: List<RecentRecordItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最近记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (records.isEmpty()) {
                Text(
                    text = "暂无记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                records.take(20).forEach { record ->
                    RecordRow(item = record)
                    if (record != records.take(20).last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(item: RecentRecordItem) {
    val dotColor = when (item.type) {
        "duty" -> Yellow
        "attendance" -> Red
        "booking" -> Green
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = DateUtils.formatDisplay(item.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(68.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
