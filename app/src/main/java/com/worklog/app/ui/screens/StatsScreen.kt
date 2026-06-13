package com.worklog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worklog.app.data.entity.AttendanceRecord
import com.worklog.app.data.entity.AttendanceType
import com.worklog.app.data.entity.BookingRecord
import com.worklog.app.data.entity.DutyRecord
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

enum class StatsTimeMode { QUARTER, MONTH, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(repository: DataRepository) {
    val employees by repository.getActiveEmployees().collectAsState(initial = emptyList())
    val attendanceTypes by repository.getAllAttendanceTypes().collectAsState(initial = emptyList())

    var timeMode by remember { mutableStateOf(StatsTimeMode.QUARTER) }
    var currentYearMonth by remember { mutableStateOf(DateUtils.currentYearMonth()) }

    val (startDate, endDate) = remember(timeMode, currentYearMonth) {
        computeDateRange(timeMode, currentYearMonth)
    }

    val allDuties by repository.getDutiesByRange(startDate, endDate)
        .collectAsState(initial = emptyList())
    val allAttendance by repository.getAttendanceByDateRange(startDate, endDate)
        .collectAsState(initial = emptyList())
    val allBookings by repository.getAllBookings().collectAsState(initial = emptyList())

    val filteredBookings = remember(allBookings, startDate, endDate) {
        allBookings.filter { it.date >= startDate && it.date <= endDate }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            Column {
                TopAppBar(title = { Text("统计") })
                TimeModeSelector(
                    timeMode = timeMode,
                    currentYearMonth = currentYearMonth,
                    onModeChange = { timeMode = it },
                    onYearMonthChange = { currentYearMonth = it }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OverviewCards(
                    dutyCount = allDuties.size,
                    attendanceCount = allAttendance.size,
                    bookingCount = filteredBookings.size
                )
            }
            item {
                DutyDistributionSection(
                    duties = allDuties,
                    employees = employees
                )
            }
            item {
                AttendanceDistributionSection(
                    records = allAttendance,
                    employees = employees,
                    attendanceTypes = attendanceTypes
                )
            }
        }
    }
}

private fun computeDateRange(mode: StatsTimeMode, yearMonth: String): Pair<String, String> {
    val now = LocalDate.now()
    return when (mode) {
        StatsTimeMode.QUARTER -> {
            val m = now.monthValue
            val quarterStartMonth = ((m - 1) / 3) * 3 + 1
            val start = "${now.year}-${quarterStartMonth.toString().padStart(2, '0')}-01"
            val endMonth = quarterStartMonth + 2
            val lastDay = YearMonth.of(now.year, endMonth).lengthOfMonth()
            val end = "${now.year}-${endMonth.toString().padStart(2, '0')}-${lastDay}"
            start to end
        }
        StatsTimeMode.MONTH -> {
            val start = DateUtils.getMonthStart(yearMonth)
            val end = DateUtils.getMonthEnd(yearMonth)
            start to end
        }
        StatsTimeMode.CUSTOM -> {
            val start = LocalDate.now().minusMonths(6).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val end = DateUtils.today()
            start to end
        }
    }
}

@Composable
private fun TimeModeSelector(
    timeMode: StatsTimeMode,
    currentYearMonth: String,
    onModeChange: (StatsTimeMode) -> Unit,
    onYearMonthChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsTimeMode.entries.forEach { mode ->
                val label = when (mode) {
                    StatsTimeMode.QUARTER -> "本季度"
                    StatsTimeMode.MONTH -> "本月"
                    StatsTimeMode.CUSTOM -> "近半年"
                }
                FilterChip(
                    selected = timeMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(label, fontSize = 13.sp) }
                )
            }
        }
        if (timeMode == StatsTimeMode.MONTH) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onYearMonthChange(DateUtils.previousMonth(currentYearMonth)) }) {
                    Text("◀")
                }
                Text(currentYearMonth, fontWeight = FontWeight.Medium)
                TextButton(onClick = { onYearMonthChange(DateUtils.nextMonth(currentYearMonth)) }) {
                    Text("▶")
                }
            }
        }
    }
}

@Composable
private fun OverviewCards(
    dutyCount: Int,
    attendanceCount: Int,
    bookingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverviewCard(
            title = "值班总人次",
            value = dutyCount.toString(),
            color = Color(0xFF007AFF),
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            title = "考勤事件数",
            value = attendanceCount.toString(),
            color = Color(0xFFFF9500),
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            title = "预约活动数",
            value = bookingCount.toString(),
            color = Color(0xFF34C759),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DutyDistributionSection(
    duties: List<DutyRecord>,
    employees: List<Employee>
) {
    val summary = remember(duties, employees) {
        val map = mutableMapOf<Long, DutyEmployeeStat>()
        employees.forEach { emp ->
            map[emp.id] = DutyEmployeeStat(emp.name, emp.color, 0, 0, 0)
        }
        duties.forEach { duty ->
            val stat = map.getOrPut(duty.employeeId) {
                val emp = employees.find { it.id == duty.employeeId }
                DutyEmployeeStat(emp?.name ?: "未知", emp?.color ?: 0xFF007AFF.toInt(), 0, 0, 0)
            }
            when (duty.shiftType) {
                "早班" -> stat.early++
                "晚班" -> stat.late++
                "通班" -> stat.full++
            }
        }
        map.values.sortedByDescending { it.total }
    }

    val maxTotal = summary.maxOfOrNull { it.total } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "值班工作量分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("姓名", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("早班", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("晚班", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("通班", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("合计", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))

            summary.forEach { stat ->
                DutyDistributionRow(stat, maxTotal)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private data class DutyEmployeeStat(
    val name: String,
    val color: Int,
    var early: Int,
    var late: Int,
    var full: Int
) {
    val total: Int get() = early + late + full
}

@Composable
private fun DutyDistributionRow(stat: DutyEmployeeStat, maxTotal: Int) {
    val progress = if (maxTotal > 0) stat.total.toFloat() / maxTotal else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    Modifier.size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(stat.color))
                )
                Spacer(Modifier.width(6.dp))
                Text(stat.name, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stat.early.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            Text(
                stat.late.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            Text(
                stat.full.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            Text(
                stat.total.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF007AFF),
            trackColor = Color(0xFF007AFF).copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun AttendanceDistributionSection(
    records: List<AttendanceRecord>,
    employees: List<Employee>,
    attendanceTypes: List<AttendanceType>
) {
    val summary = remember(records, employees, attendanceTypes) {
        val empMap = mutableMapOf<Long, MutableMap<Long, Int>>()
        employees.forEach { emp ->
            empMap[emp.id] = mutableMapOf()
        }
        records.forEach { rec ->
            val typeMap = empMap.getOrPut(rec.employeeId) { mutableMapOf() }
            typeMap[rec.typeId] = (typeMap[rec.typeId] ?: 0) + 1
        }
        empMap
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "考勤事件分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("姓名", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                attendanceTypes.forEach { type ->
                    Text(
                        type.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Text("合计", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))

            employees.forEach { emp ->
                val typeMap = summary[emp.id] ?: emptyMap()
                val total = typeMap.values.sum()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emp.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    attendanceTypes.forEach { type ->
                        val count = typeMap[type.id] ?: 0
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            color = if (count > 0) Color(type.color) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        total.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (employees.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
