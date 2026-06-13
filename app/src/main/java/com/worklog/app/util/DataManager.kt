package com.worklog.app.util

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.worklog.app.data.AppDatabase
import com.worklog.app.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val employees: List<Employee> = emptyList(),
    val dutyRecords: List<DutyRecord> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val bookingRecords: List<BookingRecord> = emptyList(),
    val venues: List<Venue> = emptyList(),
    val attendanceTypes: List<AttendanceType> = emptyList(),
    val inboundRecords: List<InboundRecord> = emptyList(),
    val exportDate: String = DateUtils.today()
)

class DataManager(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val db = AppDatabase.getInstance(context)

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val data = BackupData(
            employees = collectAll(db.employeeDao().getAll()),
            dutyRecords = collectAll(db.dutyDao().getByDateRange("2000-01-01", "2099-12-31")),
            attendanceRecords = collectAll(db.attendanceDao().getAll()),
            bookingRecords = collectAll(db.bookingDao().getAll()),
            venues = collectAll(db.venueDao().getAll()),
            attendanceTypes = collectAll(db.attendanceTypeDao().getAll()),
            inboundRecords = collectAll(db.inboundDao().getAll()),
            exportDate = DateUtils.today()
        )
        gson.toJson(data)
    }

    suspend fun importFromJson(jsonString: String): String = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<BackupData>() {}.type
            val data: BackupData = gson.fromJson(jsonString, type)

            db.clearAllTables()

            data.employees.forEach { db.employeeDao().insert(it) }
            data.dutyRecords.forEach { db.dutyDao().insert(it) }
            data.attendanceRecords.forEach { db.attendanceDao().insert(it) }
            data.bookingRecords.forEach { db.bookingDao().insert(it) }
            data.venues.forEach { db.venueDao().insert(it) }
            data.attendanceTypes.forEach { db.attendanceTypeDao().insert(it) }
            data.inboundRecords.forEach { db.inboundDao().insert(it) }

            "导入成功，共恢复 ${data.employees.size} 个员工、" +
                    "${data.dutyRecords.size} 条值班、" +
                    "${data.attendanceRecords.size} 条考勤、" +
                    "${data.bookingRecords.size} 条预约记录"
        } catch (e: Exception) {
            "导入失败：${e.message}"
        }
    }

    private suspend fun <T> collectAll(flow: kotlinx.coroutines.flow.Flow<List<T>>): List<T> {
        return flow.first()
    }
}
