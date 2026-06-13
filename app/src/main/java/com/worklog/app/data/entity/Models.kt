package com.worklog.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF007AFF.toInt(),
    val isActive: Boolean = true
)

@Entity(tableName = "duty_records")
data class DutyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val date: String,
    val shiftType: String
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val typeId: Long,
    val startDate: String,
    val endDate: String? = null,
    val note: String? = null
)

@Entity(tableName = "booking_records")
data class BookingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val department: String,
    val venueId: Long,
    val date: String,
    val startTime: String,
    val endTime: String,
    val contactId: Long,
    val participants: String = "[]",
    val note: String? = null,
    val status: String = "待确认"
)

@Entity(tableName = "venues")
data class Venue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "attendance_types")
data class AttendanceType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFFFF3B30.toInt()
)

@Entity(tableName = "inbound_records")
data class InboundRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val yearMonth: String,
    val count: Int = 0
)
