package com.worklog.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worklog.app.data.entity.AttendanceType
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.entity.Venue
import com.worklog.app.data.repository.DataRepository
import com.worklog.app.util.DataManager
import com.worklog.app.util.DateUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: DataRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataManager = remember { DataManager(context) }

    val employees by repository.getAllEmployees().collectAsState(initial = emptyList())
    val venues by repository.getAllVenues().collectAsState(initial = emptyList())
    val attendanceTypes by repository.getAllAttendanceTypes().collectAsState(initial = emptyList())

    var quickEntryEnabled by remember { mutableStateOf(true) }
    var venueOpsEnabled by remember { mutableStateOf(true) }
    var recentRecordsEnabled by remember { mutableStateOf(true) }

    var showEmployeeDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var showVenueDialog by remember { mutableStateOf(false) }
    var showAttendanceTypeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = dataManager.exportToJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    toastMessage = "导出成功"
                } catch (e: Exception) {
                    toastMessage = "导出失败：${e.message}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    } ?: throw Exception("无法读取文件")
                    val result = dataManager.importFromJson(json)
                    toastMessage = result
                } catch (e: Exception) {
                    toastMessage = "导入失败：${e.message}"
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 员工管理
            item {
                SectionCard(title = "员工管理") {
                    employees.forEach { emp ->
                        EmployeeRow(
                            employee = emp,
                            onEdit = { editingEmployee = emp; showEmployeeDialog = true },
                            onDelete = { scope.launch { repository.softDeleteEmployee(emp.id) } }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingEmployee = null
                                showEmployeeDialog = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新增员工",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("新增员工", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 功能区管理
            item {
                SectionCard(title = "功能区管理") {
                    venues.forEach { venue ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = venue.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { scope.launch { repository.deleteVenue(venue.id) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVenueDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新增功能区",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("新增功能区", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 考勤类型管理
            item {
                SectionCard(title = "考勤类型管理") {
                    attendanceTypes.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(Color(type.color))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { scope.launch { repository.deleteAttendanceType(type.id) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAttendanceTypeDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新增考勤类型",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("新增考勤类型", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 首页模块显示开关
            item {
                SectionCard(title = "首页模块显示") {
                    SettingSwitch(
                        label = "快捷录入",
                        checked = quickEntryEnabled,
                        onCheckedChange = { quickEntryEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingSwitch(
                        label = "场馆运营",
                        checked = venueOpsEnabled,
                        onCheckedChange = { venueOpsEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingSwitch(
                        label = "最近记录",
                        checked = recentRecordsEnabled,
                        onCheckedChange = { recentRecordsEnabled = it }
                    )
                }
            }

            // 数据管理
            item {
                SectionCard(title = "数据管理") {
                    DataActionRow(
                        label = "导出备份",
                        onClick = {
                            val filename = "worklog_backup_${DateUtils.today()}.json"
                            exportLauncher.launch(filename)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DataActionRow(
                        label = "导入恢复",
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DataActionRow(
                        label = "清空所有数据",
                        isDestructive = true,
                        onClick = { showClearConfirm = true }
                    )
                }
            }
        }
    }

    // 员工编辑对话框
    if (showEmployeeDialog) {
        EmployeeEditDialog(
            employee = editingEmployee,
            onDismiss = { showEmployeeDialog = false; editingEmployee = null },
            onSave = { emp ->
                scope.launch {
                    if (emp.id == 0L) repository.saveEmployee(emp)
                    else repository.updateEmployee(emp)
                }
                showEmployeeDialog = false
                editingEmployee = null
            }
        )
    }

    // 功能区新增对话框
    if (showVenueDialog) {
        AddItemDialog(
            title = "新增功能区",
            onDismiss = { showVenueDialog = false },
            onSave = { name ->
                scope.launch { repository.addVenue(Venue(name = name)) }
                showVenueDialog = false
            }
        )
    }

    // 考勤类型新增对话框
    if (showAttendanceTypeDialog) {
        AddAttendanceTypeDialog(
            onDismiss = { showAttendanceTypeDialog = false },
            onSave = { type ->
                scope.launch { repository.addAttendanceType(type) }
                showAttendanceTypeDialog = false
            }
        )
    }

    // 清空确认对话框
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空所有数据") },
            text = { Text("此操作将删除所有数据且不可恢复，确定要清空吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.worklog.app.data.AppDatabase.getInstance(context).clearAllTables()
                        }
                    }
                    showClearConfirm = false
                }) {
                    Text("确定清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    // Toast提示
    toastMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { Text("提示") },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) { Text("确定") }
            },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EmployeeRow(
    employee: Employee,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background(Color(employee.color))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (!employee.isActive) {
                Text(
                    text = "已禁用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DataActionRow(
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmployeeEditDialog(
    employee: Employee?,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var selectedColor by remember { mutableStateOf(employee?.color ?: 0xFF007AFF.toInt()) }

    val colorOptions = listOf(
        0xFF007AFF.toInt(), 0xFF34C759.toInt(), 0xFFFF9500.toInt(),
        0xFFFF3B30.toInt(), 0xFFAF52DE.toInt(), 0xFFFFCC00.toInt(),
        0xFF5AC8FA.toInt(), 0xFF8E8E93.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee != null) "编辑员工" else "新增员工") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("选择颜色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            Employee(
                                id = employee?.id ?: 0,
                                name = name,
                                color = selectedColor,
                                isActive = employee?.isActive ?: true
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddItemDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddAttendanceTypeDialog(
    onDismiss: () -> Unit,
    onSave: (AttendanceType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFFFF3B30.toInt()) }

    val colorOptions = listOf(
        0xFFFF3B30.toInt(), 0xFFFF9500.toInt(), 0xFFFFCC00.toInt(),
        0xFF34C759.toInt(), 0xFF007AFF.toInt(), 0xFFAF52DE.toInt(),
        0xFF5AC8FA.toInt(), 0xFF8E8E93.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增考勤类型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("类型名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("选择颜色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(AttendanceType(name = name, color = selectedColor))
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
