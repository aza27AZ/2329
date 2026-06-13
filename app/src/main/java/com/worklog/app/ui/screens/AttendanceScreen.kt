package com.worklog.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worklog.app.data.entity.AttendanceRecord
import com.worklog.app.data.entity.AttendanceType
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.util.DateUtils
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(repository: DataRepository) {
    val scope = rememberCoroutineScope()
    val employees by repository.getActiveEmployees().collectAsState(initial = emptyList())
    val attendanceTypes by repository.getAllAttendanceTypes().collectAsState(initial = emptyList())
    val attendanceRecords by repository.getAllAttendance().collectAsState(initial = emptyList())

    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var isCalendarView by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(DateUtils.currentYearMonth()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }

    val filteredRecords = remember(attendanceRecords, selectedTypeId) {
        if (selectedTypeId == null) attendanceRecords
        else attendanceRecords.filter { it.typeId == selectedTypeId }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            Column {
                TopAppBar(title = { Text("考勤管理") })
                TypeFilterBar(
                    types = attendanceTypes,
                    selectedTypeId = selectedTypeId,
                    onTypeSelected = { selectedTypeId = it }
                )
                ViewToggle(
                    isCalendarView = isCalendarView,
                    onToggle = { isCalendarView = it }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加考勤")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (isCalendarView) {
                AttendanceCalendarView(
                    yearMonth = currentYearMonth,
                    records = filteredRecords,
                    attendanceTypes = attendanceTypes,
                    onPreviousMonth = { currentYearMonth = DateUtils.previousMonth(currentYearMonth) },
                    onNextMonth = { currentYearMonth = DateUtils.nextMonth(currentYearMonth) }
                )
            } else {
                AttendanceListView(
                    records = filteredRecords,
                    employees = employees,
                    attendanceTypes = attendanceTypes,
                    onEdit = { editingRecord = it; showDialog = true },
                    onDelete = { id -> scope.launch { repository.deleteAttendanceRecord(id) } }
                )
            }
        }
    }

    if (showDialog) {
        AttendanceDialog(
            record = editingRecord,
            employees = employees,
            attendanceTypes = attendanceTypes,
            onDismiss = { showDialog = false; editingRecord = null },
            onSave = { record ->
                scope.launch {
                    if (editingRecord != null) {
                        repository.updateAttendanceRecord(record)
                    } else {
                        repository.addAttendanceRecord(record)
                    }
                }
                showDialog = false
                editingRecord = null
            }
        )
    }
}

@Composable
private fun TypeFilterBar(
    types: List<AttendanceType>,
    selectedTypeId: Long?,
    onTypeSelected: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTypeId == null,
            onClick = { onTypeSelected(null) },
            label = { Text("全部") }
        )
        types.forEach { type ->
            FilterChip(
                selected = selectedTypeId == type.id,
                onClick = { onTypeSelected(type.id) },
                label = { Text(type.name) },
                leadingIcon = {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(Color(type.color))
                    )
                }
            )
        }
    }
}

@Composable
private fun ViewToggle(isCalendarView: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        SegmentedButton(
            selected = !isCalendarView,
            onClick = { onToggle(false) },
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("列表", fontSize = 12.sp)
        }
        SegmentedButton(
            selected = isCalendarView,
            onClick = { onToggle(true) },
            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("日历", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    content: @Composable RowScope.() -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "bg"
    )
    val textColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "textColor"
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = bg,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun AttendanceListView(
    records: List<AttendanceRecord>,
    employees: List<Employee>,
    attendanceTypes: List<AttendanceType>,
    onEdit: (AttendanceRecord) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.id }) { record ->
            val employee = employees.find { it.id == record.employeeId }
            val type = attendanceTypes.find { it.id == record.typeId }
            AttendanceCard(
                record = record,
                employee = employee,
                type = type,
                onEdit = { onEdit(record) },
                onDelete = { onDelete(record.id) }
            )
        }
    }
}

@Composable
private fun AttendanceCard(
    record: AttendanceRecord,
    employee: Employee?,
    type: AttendanceType?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Color(employee?.color ?: 0xFF007AFF.toInt())),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee?.name?.take(1) ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = employee?.name ?: "未知",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(Color(type?.color ?: 0xFF007AFF.toInt()))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = type?.name ?: "未知类型",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateUtils.formatDisplay(record.startDate),
                    style = MaterialTheme.typography.bodySmall
                )
                if (record.endDate != null && record.endDate != record.startDate) {
                    Text(
                        text = "至 ${DateUtils.formatDisplayFull(record.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!record.note.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 80.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceCalendarView(
    yearMonth: String,
    records: List<AttendanceRecord>,
    attendanceTypes: List<AttendanceType>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val days = DateUtils.getDateList(yearMonth)
    val firstDayOfWeek = DateUtils.getFirstDayOfWeek(yearMonth)
    val dateToRecords = remember(records) {
        records.groupBy { it.startDate }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviousMonth) { Text("◀") }
            Text(
                text = yearMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNextMonth) { Text("▶") }
        }

        val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val leadingSpacers = (0 until firstDayOfWeek).map { "" }
        val allCells = leadingSpacers + days

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            allCells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { dayStr ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayStr.isNotEmpty()) {
                                val day = dayStr.split("-").last().toIntOrNull() ?: 0
                                val dayRecords = dateToRecords[dayStr].orEmpty()
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (dayRecords.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dayRecords.forEach { rec ->
                                                val type = attendanceTypes.find { it.id == rec.typeId }
                                                Box(
                                                    Modifier.size(5.dp).clip(CircleShape)
                                                        .background(Color(type?.color ?: 0xFF007AFF.toInt()))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceDialog(
    record: AttendanceRecord?,
    employees: List<Employee>,
    attendanceTypes: List<AttendanceType>,
    onDismiss: () -> Unit,
    onSave: (AttendanceRecord) -> Unit
) {
    var selectedEmployeeId by remember { mutableStateOf(record?.employeeId ?: employees.firstOrNull()?.id ?: 0L) }
    var selectedTypeId by remember { mutableStateOf(record?.typeId ?: attendanceTypes.firstOrNull()?.id ?: 0L) }
    var startDate by remember { mutableStateOf(record?.startDate ?: DateUtils.today()) }
    var endDate by remember { mutableStateOf(record?.endDate ?: "") }
    var note by remember { mutableStateOf(record?.note ?: "") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record != null) "编辑考勤" else "登记考勤") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 人员选择
                Text("人员", style = MaterialTheme.typography.labelMedium)
                employees.forEach { emp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEmployeeId = emp.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEmployeeId == emp.id,
                            onClick = { selectedEmployeeId = emp.id }
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                             Modifier.size(8.dp).clip(CircleShape)
                                 .background(Color(emp.color))
                         )
                        Spacer(Modifier.width(6.dp))
                        Text(emp.name)
                    }
                }

                // 考勤类型（彩色按钮组）
                Text("事件类型", style = MaterialTheme.typography.labelMedium)
                AttendanceTypeButtonGroup(
                    types = attendanceTypes,
                    selectedTypeId = selectedTypeId,
                    onTypeSelected = { selectedTypeId = it }
                )

                // 日期范围
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("开始日期", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                            trailingIcon = { Text("📅") }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("结束日期(可选)", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true },
                            trailingIcon = { Text("📅") }
                        )
                    }
                }

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AttendanceRecord(
                            id = record?.id ?: 0,
                            employeeId = selectedEmployeeId,
                            typeId = selectedTypeId,
                            startDate = startDate,
                            endDate = endDate.ifBlank { null },
                            note = note.ifBlank { null }
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    if (showStartDatePicker) {
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDate(startDate)
                ?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000L))
                        startDate = date.format(dateFormatter)
                    }
                    showStartDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndDatePicker) {
        val endPickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDate(endDate.ifBlank { DateUtils.today() })
                ?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000L))
                        endDate = date.format(dateFormatter)
                    }
                    showEndDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun AttendanceTypeButtonGroup(
    types: List<AttendanceType>,
    selectedTypeId: Long,
    onTypeSelected: (Long) -> Unit
) {
    @Composable
    fun TypeButton(type: AttendanceType) {
        val isSelected = selectedTypeId == type.id
        val bg by animateColorAsState(
            if (isSelected) Color(type.color) else Color(type.color).copy(alpha = 0.15f),
            label = "typeBg"
        )
        val textColor by animateColorAsState(
            if (isSelected) Color.White else Color(type.color),
            label = "typeTextColor"
        )
        Surface(
            onClick = { onTypeSelected(type.id) },
            shape = RoundedCornerShape(8.dp),
            color = bg
        ) {
            Text(
                text = type.name,
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        types.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { TypeButton(it) }
            }
        }
    }
}
