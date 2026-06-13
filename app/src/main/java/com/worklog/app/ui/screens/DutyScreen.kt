package com.worklog.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.worklog.app.data.entity.DutyRecord
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.ui.theme.DutyEvening
import com.worklog.app.ui.theme.DutyFull
import com.worklog.app.ui.theme.DutyMorning
import com.worklog.app.ui.theme.GrayBackground
import com.worklog.app.util.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class DutyStats(
    val employeeId: Long,
    val name: String,
    val earlyCount: Int = 0,
    val lateCount: Int = 0,
    val fullCount: Int = 0
) {
    val total: Int get() = earlyCount + lateCount + fullCount
}

private data class DayDutyInfo(
    val date: String,
    val dayOfMonth: Int,
    val duties: List<Pair<Employee, DutyRecord>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutyScreen(repository: DataRepository) {
    val scope = rememberCoroutineScope()
    val employees by repository.getActiveEmployees().collectAsState(initial = emptyList())

    var currentYearMonth by remember { mutableStateOf(DateUtils.currentYearMonth()) }
    var statsExpanded by remember { mutableStateOf(true) }
    var selectedDayInfo by remember { mutableStateOf<DayDutyInfo?>(null) }
    var showBatchDialog by remember { mutableStateOf(false) }

    val monthStart = remember(currentYearMonth) { "$currentYearMonth-01" }
    val monthEnd = remember(currentYearMonth) { DateUtils.getMonthEnd(currentYearMonth) }

    val dutyRecords by repository.getDutiesByRange(monthStart, monthEnd)
        .collectAsState(initial = emptyList())

    val employeeMap = remember(employees) {
        employees.associateBy { it.id }
    }

    val stats = remember(employees, dutyRecords) {
        employees.map { emp ->
            val empRecords = dutyRecords.filter { it.employeeId == emp.id }
            DutyStats(
                employeeId = emp.id,
                name = emp.name,
                earlyCount = empRecords.count { it.shiftType == "早班" },
                lateCount = empRecords.count { it.shiftType == "晚班" },
                fullCount = empRecords.count { it.shiftType == "通班" }
            )
        }
    }

    val maxTotal = remember(stats) { stats.maxOfOrNull { it.total } ?: 1 }

    val dateToDuties = remember(dutyRecords, employeeMap) {
        dutyRecords.groupBy { it.date }.mapValues { (_, records) ->
            records.mapNotNull { rec ->
                employeeMap[rec.employeeId]?.let { emp -> emp to rec }
            }
        }
    }

    val monthDays = remember(currentYearMonth) { DateUtils.getDateList(currentYearMonth) }
    val scheduledDayCount = remember(dutyRecords) {
        dutyRecords.map { it.date }.distinct().size
    }
    val totalDaysInMonth = remember(currentYearMonth) { DateUtils.getMonthDays(currentYearMonth) }

    val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val firstDayOffset = remember(currentYearMonth) {
        val parts = currentYearMonth.split("-")
        val firstDate = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        (firstDate.dayOfWeek.value + 6) % 7
    }

    Scaffold(
        containerColor = GrayBackground,
        topBar = {
            TopAppBar(
                title = { Text("值班排班") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBatchDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "批量添加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            MonthNavigation(
                currentYearMonth = currentYearMonth,
                onPrevious = { currentYearMonth = DateUtils.previousMonth(currentYearMonth) },
                onNext = { currentYearMonth = DateUtils.nextMonth(currentYearMonth) }
            )

            CompletionProgressCard(
                scheduledCount = scheduledDayCount,
                totalDays = totalDaysInMonth
            )

            CollapsibleStatsPanel(
                expanded = statsExpanded,
                onToggle = { statsExpanded = !statsExpanded },
                stats = stats,
                maxTotal = maxTotal
            )

            CalendarSection(
                yearMonth = currentYearMonth,
                monthDays = monthDays,
                firstDayOffset = firstDayOffset,
                dayNames = dayNames,
                dateToDuties = dateToDuties,
                onDayClick = { date ->
                    val duties = dateToDuties[date].orEmpty()
                    if (duties.isNotEmpty()) {
                        val dayOfMonth = date.split("-").last().toIntOrNull() ?: 0
                        selectedDayInfo = DayDutyInfo(
                            date = date,
                            dayOfMonth = dayOfMonth,
                            duties = duties
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (selectedDayInfo != null) {
        DayDetailDialog(
            info = selectedDayInfo!!,
            onDismiss = { selectedDayInfo = null }
        )
    }

    if (showBatchDialog) {
        BatchAddDialog(
            yearMonth = currentYearMonth,
            employees = employees,
            stats = stats,
            onDismiss = { showBatchDialog = false },
            onConfirm = { records ->
                scope.launch {
                    val existingPairs = dutyRecords.map { "${it.employeeId}_${it.date}" }.toSet()
                    val uniqueRecords = records.filter {
                        "${it.employeeId}_${it.date}" !in existingPairs
                    }
                    if (uniqueRecords.isNotEmpty()) {
                        repository.addDutyRecords(uniqueRecords)
                    }
                }
                showBatchDialog = false
            }
        )
    }
}

@Composable
private fun MonthNavigation(
    currentYearMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "上月",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = currentYearMonth,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "下月",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CompletionProgressCard(
    scheduledCount: Int,
    totalDays: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "排班完成度",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "已排 $scheduledCount/$totalDays 天",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val progress = if (totalDays > 0) scheduledCount.toFloat() / totalDays else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun CollapsibleStatsPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    stats: List<DutyStats>,
    maxTotal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当月值班累计",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (stats.isEmpty()) {
                        Text(
                            text = "暂无员工数据",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        stats.forEach { stat ->
                            EmployeeStatRow(stat = stat, maxTotal = maxTotal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeStatRow(
    stat: DutyStats,
    maxTotal: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    if (stat.earlyCount > 0) append("早${stat.earlyCount} ")
                    if (stat.lateCount > 0) append("晚${stat.lateCount} ")
                    if (stat.fullCount > 0) append("通${stat.fullCount} ")
                    append("合计${stat.total}")
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val ratio = if (maxTotal > 0) stat.total.toFloat() / maxTotal else 0f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ShiftDotsRow(stat = stat)
        }
    }
}

@Composable
private fun ShiftDotsRow(stat: DutyStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (stat.earlyCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DutyMorning)
            )
        }
        if (stat.lateCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DutyEvening)
            )
        }
        if (stat.fullCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DutyFull)
            )
        }
    }
}

@Composable
private fun CalendarSection(
    yearMonth: String,
    monthDays: List<String>,
    firstDayOffset: Int,
    dayNames: List<String>,
    dateToDuties: Map<String, List<Pair<Employee, DutyRecord>>>,
    onDayClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (day == "日") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val blankCells = (0 until firstDayOffset).map { "" }
            val allCells = blankCells + monthDays

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                allCells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { dateStr ->
                            CalendarDayCell(
                                dateStr = dateStr,
                                duties = if (dateStr.isNotEmpty()) dateToDuties[dateStr].orEmpty() else emptyList(),
                                isToday = dateStr == DateUtils.today(),
                                onClick = { onDayClick(dateStr) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dateStr: String,
    duties: List<Pair<Employee, DutyRecord>>,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNumber = dateStr.split("-").lastOrNull()?.toIntOrNull()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                else Modifier
            )
            .clickable(enabled = dayNumber != null && duties.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (dayNumber != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayNumber.toString(),
                    fontSize = if (isToday) 15.sp else 13.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                if (duties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shiftTypes = duties.map { it.second.shiftType }.distinct()
                        shiftTypes.forEach { shift ->
                            val dotColor = when (shift) {
                                "早班" -> DutyMorning
                                "晚班" -> DutyEvening
                                "通班" -> DutyFull
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailDialog(
    info: DayDutyInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = DateUtils.formatDisplayFull(info.date),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "值班人员 (${info.duties.size}人)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                info.duties.forEach { (employee, duty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val chipColor = when (duty.shiftType) {
                            "早班" -> DutyMorning
                            "晚班" -> DutyEvening
                            "通班" -> DutyFull
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = chipColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = duty.shiftType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = chipColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchAddDialog(
    yearMonth: String,
    employees: List<Employee>,
    stats: List<DutyStats>,
    onDismiss: () -> Unit,
    onConfirm: (List<DutyRecord>) -> Unit
) {
    val statMap = remember(stats) { stats.associateBy { it.employeeId } }

    var selectedShiftType by remember { mutableStateOf("早班") }
    var selectedEmployeeIds by remember { mutableStateOf(setOf<Long>()) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseDate("$yearMonth-01")
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseDate(DateUtils.getMonthEnd(yearMonth))
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    val shiftTypes = listOf("早班", "晚班", "通班")

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val dateRangeSize = remember(startDate, endDate) {
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            generateDateRange(startDate, endDate).size
        } else 0
    }

    val canConfirm = selectedShiftType.isNotBlank() &&
            selectedEmployeeIds.isNotEmpty() &&
            startDate.isNotBlank() && endDate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "批量添加排班",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "班次选择",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shiftTypes.forEach { shift ->
                        val chipColor = when (shift) {
                            "早班" -> DutyMorning
                            "晚班" -> DutyEvening
                            "通班" -> DutyFull
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val isSelected = selectedShiftType == shift
                        Surface(
                            onClick = { selectedShiftType = shift },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) chipColor else Color.Transparent,
                            border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
                        ) {
                            Text(
                                text = shift,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else chipColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "日期范围",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("开始日期") },
                        placeholder = { Text("选择日期") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartPicker = true },
                        trailingIcon = {
                            Text("📅", fontSize = 16.sp)
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("结束日期") },
                        placeholder = { Text("选择日期") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndPicker = true },
                        trailingIcon = {
                            Text("📅", fontSize = 16.sp)
                        },
                        singleLine = true
                    )
                }

                Text(
                    text = "选择员工",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedEmployeeIds.isNotEmpty()) {
                    Text(
                        text = "已选 ${selectedEmployeeIds.size} 人",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(employees) { employee ->
                        val empStat = statMap[employee.id]
                        val totalCount = empStat?.total ?: 0
                        val isSelected = employee.id in selectedEmployeeIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedEmployeeIds = if (isSelected) {
                                        selectedEmployeeIds - employee.id
                                    } else {
                                        selectedEmployeeIds + employee.id
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedEmployeeIds = if (checked) {
                                        selectedEmployeeIds + employee.id
                                    } else {
                                        selectedEmployeeIds - employee.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(Color(employee.color))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = employee.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "已排 $totalCount 次",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dates = generateDateRange(startDate, endDate)
                    if (dates.isEmpty()) return@TextButton

                    val records = mutableListOf<DutyRecord>()
                    selectedEmployeeIds.forEach { empId ->
                        dates.forEach { date ->
                            records.add(
                                DutyRecord(
                                    employeeId = empId,
                                    date = date,
                                    shiftType = selectedShiftType
                                )
                            )
                        }
                    }
                    onConfirm(records)
                },
                enabled = canConfirm
            ) {
                Text("提交 (${selectedEmployeeIds.size}人 × ${dateRangeSize}天)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startPickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(
                                millis / (24 * 60 * 60 * 1000L)
                            )
                            startDate = date.format(dateFormatter)
                        }
                        showStartPicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endPickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(
                                millis / (24 * 60 * 60 * 1000L)
                            )
                            endDate = date.format(dateFormatter)
                        }
                        showEndPicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

private fun generateDateRange(startDate: String, endDate: String): List<String> {
    val start = DateUtils.parseDate(startDate) ?: return emptyList()
    val end = DateUtils.parseDate(endDate) ?: return emptyList()
    if (start.isAfter(end)) return emptyList()

    val dates = mutableListOf<String>()
    var current = start
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    while (!current.isAfter(end)) {
        dates.add(current.format(formatter))
        current = current.plusDays(1)
    }
    return dates
}
