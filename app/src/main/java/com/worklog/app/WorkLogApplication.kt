package com.worklog.app

import android.app.Application
import com.worklog.app.data.AppDatabase
import com.worklog.app.data.entity.AttendanceType
import com.worklog.app.data.entity.Employee
import com.worklog.app.data.entity.Venue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkLogApplication : Application() {

    lateinit var database: AppDatabase
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        initializeData()
    }

    private fun initializeData() {
        applicationScope.launch {
            val db = database
            if (db.employeeDao().getAll().let { kotlinx.coroutines.flow.first(it) }.isEmpty()) {
                db.employeeDao().insert(Employee(name = "张三", color = 0xFF007AFF.toInt()))
                db.employeeDao().insert(Employee(name = "李四", color = 0xFF34C759.toInt()))
                db.employeeDao().insert(Employee(name = "王五", color = 0xFFFF9500.toInt()))
                db.employeeDao().insert(Employee(name = "赵六", color = 0xFFAF52DE.toInt()))
            }
            if (db.venueDao().getAll().let { kotlinx.coroutines.flow.first(it) }.isEmpty()) {
                db.venueDao().insert(Venue(name = "篮球馆"))
                db.venueDao().insert(Venue(name = "羽毛球馆"))
                db.venueDao().insert(Venue(name = "会议室A"))
            }
            if (db.attendanceTypeDao().getAll().let { kotlinx.coroutines.flow.first(it) }.isEmpty()) {
                db.attendanceTypeDao().insert(AttendanceType(name = "迟到", color = 0xFFFF3B30.toInt()))
                db.attendanceTypeDao().insert(AttendanceType(name = "早退", color = 0xFFFF9500.toInt()))
                db.attendanceTypeDao().insert(AttendanceType(name = "请假", color = 0xFFFFCC00.toInt()))
                db.attendanceTypeDao().insert(AttendanceType(name = "旷工", color = 0xFFAF52DE.toInt()))
                db.attendanceTypeDao().insert(AttendanceType(name = "加班", color = 0xFF34C759.toInt()))
            }
        }
    }
}
