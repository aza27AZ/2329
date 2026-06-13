package com.worklog.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.worklog.app.data.entity.BookingRecord
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.entity.Venue
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.util.DateUtils
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(repository: DataRepository) {
    val scope = rememberCoroutineScope()
    val employees by repository.getActiveEmployees().collectAsState(initial = emptyList())
    val venues by repository.getAllVenues().collectAsState(initial = emptyList())
    val allBookings by repository.getAllBookings().collectAsState(initial = emptyList())

    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var isCalendarView by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(DateUtils.currentYearMonth()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<BookingRecord?>(null) }

    val filteredBookings = remember(allBookings, selectedStatus) {
        if (selectedStatus == null) allBookings
        else allBookings.filter { it.status == selectedStatus }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            Column {
                TopAppBar(title = { Text("活动预约") })
                StatusFilterBar(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it }
                )
                ViewToggle(
                    isCalendarView = isCalendarView,
                    onToggle = { isCalendarView = it }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建预约")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (isCalendarView) {
                BookingCalendarView(
                    yearMonth = currentYearMonth,
                    records = filteredBookings,
                    onPreviousMonth = { currentYearMonth = DateUtils.previousMonth(currentYearMonth) },
                    onNextMonth = { currentYearMonth = DateUtils.nextMonth(currentYearMonth) }
                )
            } else {
                BookingListView(
                    records = filteredBookings,
                    employees = employees,
                    venues = venues,
                    onEdit = { editingRecord = it; showDialog = true },
                    onDelete = { id -> scope.launch { repository.deleteBookingRecord(id) } },
                    onStatusChange = { record, newStatus ->
                        scope.launch {
                            repository.updateBookingRecord(record.copy(status = newStatus))
                        }
                    }
                )
            }
        }
    }

    if (showDialog) {
        BookingDialog(
            record = editingRecord,
            employees = employees,
            venues = venues,
            repository = repository,
            onDismiss = { showDialog = false; editingRecord = null },
            onSave = { record ->
                scope.launch {
                    if (editingRecord != null) {
                        repository.updateBookingRecord(record)
                    } else {
                        repository.addBookingRecord(record)
                    }
                }
                showDialog = false
                editingRecord = null
            }
        )
    }
}

private val statusOptions = listOf("全部", "待确认", "已确认", "已取消")

@Composable
private fun StatusFilterBar(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statusOptions.forEach { status ->
            val isSelected = if (status == "全部") selectedStatus == null else selectedStatus == status
            FilterChip(
                selected = isSelected,
                onClick = {
                    onStatusSelected(if (status == "全部") null else status)
                },
                label = { Text(status) }
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
private fun BookingListView(
    records: List<BookingRecord>,
    employees: List<Employee>,
    venues: List<Venue>,
    onEdit: (BookingRecord) -> Unit,
    onDelete: (Long) -> Unit,
    onStatusChange: (BookingRecord, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.id }) { record ->
            val venue = venues.find { it.id == record.venueId }
            val contact = employees.find { it.id == record.contactId }
            val participants = parseParticipants(record.participants, employees)
            BookingCard(
                record = record,
                venue = venue,
                contact = contact,
                participants = participants,
                onEdit = { onEdit(record) },
                onDelete = { onDelete(record.id) },
                onStatusChange = { newStatus -> onStatusChange(record, newStatus) }
            )
        }
    }
}

private fun parseParticipants(json: String, employees: List<Employee>): List<Employee> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            employees.find { it.id == arr.getLong(i) }
        }
    } catch (e: Exception) { emptyList() }
}

@Composable
private fun BookingCard(
    record: BookingRecord,
    venue: Venue?,
    contact: Employee?,
    participants: List<Employee>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val statusColor = when (record.status) {
        "待确认" -> Color(0xFFFF9500)
        "已确认" -> Color(0xFF34C759)
        "已取消" -> Color(0xFF8E8E93)
        else -> Color(0xFF8E8E93)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = record.department,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = record.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = venue?.name ?: "未知场馆",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${record.date} ${record.startTime}-${record.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "对接人：${contact?.name ?: "未知"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (participants.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "参与：${participants.joinToString("、") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!record.note.isNullOrBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.status == "待确认") {
                    TextButton(onClick = { onStatusChange("已确认") }) {
                        Text("确认", color = Color(0xFF34C759), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onStatusChange("已取消") }) {
                        Text("取消", color = Color(0xFFFF3B30), fontSize = 12.sp)
                    }
                } else if (record.status == "已确认") {
                    TextButton(onClick = { onStatusChange("已取消") }) {
                        Text("取消", color = Color(0xFFFF3B30), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onStatusChange("待确认") }) {
                        Text("待确认", color = Color(0xFFFF9500), fontSize = 12.sp)
                    }
                } else {
                    TextButton(onClick = { onStatusChange("待确认") }) {
                        Text("恢复", color = Color(0xFF007AFF), fontSize = 12.sp)
                    }
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
}

@Composable
private fun BookingCalendarView(
    yearMonth: String,
    records: List<BookingRecord>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val days = DateUtils.getDateList(yearMonth)
    val firstDayOfWeek = DateUtils.getFirstDayOfWeek(yearMonth)
    val dateToRecords = remember(records) {
        records.groupBy { it.date }
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
                                                val dotColor = when (rec.status) {
                                                    "待确认" -> Color(0xFFFF9500)
                                                    "已确认" -> Color(0xFF34C759)
                                                    "已取消" -> Color(0xFF8E8E93)
                                                    else -> Color(0xFF8E8E93)
                                                }
                                                Box(
                                                    Modifier.size(5.dp).clip(CircleShape)
                                                        .background(dotColor)
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
private fun BookingDialog(
    record: BookingRecord?,
    employees: List<Employee>,
    venues: List<Venue>,
    repository: DataRepository,
    onDismiss: () -> Unit,
    onSave: (BookingRecord) -> Unit
) {
    val scope = rememberCoroutineScope()
    var department by remember { mutableStateOf(record?.department ?: "") }
    var selectedVenueId by remember { mutableStateOf(record?.venueId ?: venues.firstOrNull()?.id ?: 0L) }
    var date by remember { mutableStateOf(record?.date ?: DateUtils.today()) }
    var startTime by remember { mutableStateOf(record?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(record?.endTime ?: "10:00") }
    var selectedContactId by remember { mutableStateOf(record?.contactId ?: employees.firstOrNull()?.id ?: 0L) }
    var selectedParticipantIds by remember {
        mutableStateOf(parseParticipantIds(record?.participants))
    }
    var note by remember { mutableStateOf(record?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }
    var pendingSaveRecord by remember { mutableStateOf<BookingRecord?>(null) }

    fun checkAndSave(finalRecord: BookingRecord) {
        scope.launch {
            val conflicts = repository.getConflictBookings(finalRecord.venueId, finalRecord.date)
                .filter { it.id != finalRecord.id }
                .filter { existing ->
                    val existStart = existing.startTime.replace(":", "").toIntOrNull() ?: 0
                    val existEnd = existing.endTime.replace(":", "").toIntOrNull() ?: 0
                    val newStart = finalRecord.startTime.replace(":", "").toIntOrNull() ?: 0
                    val newEnd = finalRecord.endTime.replace(":", "").toIntOrNull() ?: 0
                    !(newEnd <= existStart || newStart >= existEnd)
                }
            if (conflicts.isNotEmpty()) {
                val venue = venues.find { it.id == finalRecord.venueId }
                conflictMessage = "以下预约与您的时间段冲突：\n" +
                        conflicts.joinToString("\n") { c ->
                            "${c.department} ${c.startTime}-${c.endTime}"
                        }
                pendingSaveRecord = finalRecord
                showConflictDialog = true
            } else {
                onSave(finalRecord)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record != null) "编辑预约" else "新建预约") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("单位") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("功能区", style = MaterialTheme.typography.labelMedium)
                venues.forEach { venue ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedVenueId = venue.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVenueId == venue.id,
                            onClick = { selectedVenueId = venue.id }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(venue.name)
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = { Text("📅") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("开始时间") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("结束时间") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("对接人", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    employees.forEach { emp ->
                        FilterChip(
                            selected = selectedContactId == emp.id,
                            onClick = { selectedContactId = emp.id },
                            label = { Text(emp.name) },
                            leadingIcon = {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(Color(emp.color))
                                )
                            }
                        )
                    }
                }

                Text("参与人员（可多选）", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    employees.forEach { emp ->
                        FilterChip(
                            selected = selectedParticipantIds.contains(emp.id),
                            onClick = {
                                selectedParticipantIds = if (selectedParticipantIds.contains(emp.id)) {
                                    selectedParticipantIds - emp.id
                                } else {
                                    selectedParticipantIds + emp.id
                                }
                            },
                            label = { Text(emp.name) },
                            leadingIcon = {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(Color(emp.color))
                                )
                            }
                        )
                    }
                }

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
                    val finalRecord = BookingRecord(
                        id = record?.id ?: 0,
                        department = department,
                        venueId = selectedVenueId,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        contactId = selectedContactId,
                        participants = buildParticipantsJson(selectedParticipantIds),
                        note = note.ifBlank { null },
                        status = record?.status ?: "待确认"
                    )
                    checkAndSave(finalRecord)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDate(date)
                ?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = java.time.LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000L))
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("时间冲突") },
            text = { Text(conflictMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showConflictDialog = false
                    pendingSaveRecord?.let { onSave(it) }
                }) { Text("强制保存") }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun parseParticipantIds(participants: String?): List<Long> {
    if (participants.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(participants)
        (0 until arr.length()).map { arr.getLong(it) }
    } catch (e: Exception) { emptyList() }
}

private fun buildParticipantsJson(ids: List<Long>): String {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    return arr.toString()
}
