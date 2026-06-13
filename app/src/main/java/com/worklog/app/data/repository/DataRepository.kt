package com.worklog.app.data.repository

import com.worklog.app.data.AppDatabase
import com.worklog.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DataRepository(private val db: AppDatabase) {

    private val employeeDao = db.employeeDao()
    private val dutyDao = db.dutyDao()
    private val attendanceDao = db.attendanceDao()
    private val bookingDao = db.bookingDao()
    private val venueDao = db.venueDao()
    private val attendanceTypeDao = db.attendanceTypeDao()
    private val inboundDao = db.inboundDao()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getYearMonth(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    fun getToday(): String = LocalDate.now().format(dateFormatter)
    fun getMonthStart(yearMonth: String): String = "$yearMonth-01"
    fun getMonthEnd(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val lastDay = java.time.YearMonth.of(year, month).lengthOfMonth()
        return "$yearMonth-${lastDay.toString().padStart(2, '0')}"
    }

    // Employee
    fun getAllEmployees(): Flow<List<Employee>> = employeeDao.getAll()
    fun getActiveEmployees(): Flow<List<Employee>> = employeeDao.getActive()
    suspend fun getEmployeeById(id: Long): Employee? = employeeDao.getById(id)
    suspend fun saveEmployee(employee: Employee): Long = employeeDao.insert(employee)
    suspend fun updateEmployee(employee: Employee) = employeeDao.update(employee)
    suspend fun softDeleteEmployee(id: Long) = employeeDao.softDelete(id)
    suspend fun restoreEmployee(id: Long) = employeeDao.restore(id)

    // Duty
    fun getDutiesByRange(start: String, end: String): Flow<List<DutyRecord>> =
        dutyDao.getByDateRange(start, end)

    suspend fun getDutiesByDate(date: String): List<DutyRecord> = dutyDao.getByDate(date)
    suspend fun addDutyRecord(record: DutyRecord): Long = dutyDao.insert(record)
    suspend fun addDutyRecords(records: List<DutyRecord>) = dutyDao.insertAll(records)
    suspend fun deleteDutyRecord(id: Long) = dutyDao.deleteById(id)

    suspend fun getShiftCounts(employeeId: Long, start: String, end: String) =
        dutyDao.getShiftCounts(employeeId, start, end)

    suspend fun checkDuplicateDuty(employeeId: Long, date: String): Boolean {
        return dutyDao.getByEmployeeAndDate(employeeId, date) != null
    }

    data class EmployeeSummary(
        val employeeId: Long,
        val name: String,
        val earlyCount: Int = 0,
        val lateCount: Int = 0,
        val fullCount: Int = 0
    ) {
        val total: Int get() = earlyCount + lateCount + fullCount
    }

    // Attendance
    fun getAllAttendance(): Flow<List<AttendanceRecord>> = attendanceDao.getAll()
    fun getAttendanceByType(typeId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getByType(typeId)
    fun getAttendanceByDateRange(start: String, end: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getByDateRange(start, end)
    suspend fun addAttendanceRecord(record: AttendanceRecord): Long =
        attendanceDao.insert(record)
    suspend fun updateAttendanceRecord(record: AttendanceRecord) =
        attendanceDao.update(record)
    suspend fun deleteAttendanceRecord(id: Long) = attendanceDao.deleteById(id)

    // Booking
    fun getAllBookings(): Flow<List<BookingRecord>> = bookingDao.getAll()
    fun getBookingsByStatus(status: String): Flow<List<BookingRecord>> =
        bookingDao.getByStatus(status)
    suspend fun getBookingsByDate(date: String): List<BookingRecord> =
        bookingDao.getByDate(date)
    suspend fun getConflictBookings(venueId: Long, date: String): List<BookingRecord> =
        bookingDao.getByVenueAndDate(venueId, date)
    suspend fun addBookingRecord(record: BookingRecord): Long = bookingDao.insert(record)
    suspend fun updateBookingRecord(record: BookingRecord) = bookingDao.update(record)
    suspend fun deleteBookingRecord(id: Long) = bookingDao.deleteById(id)

    // Venue
    fun getAllVenues(): Flow<List<Venue>> = venueDao.getAll()
    suspend fun getVenueById(id: Long): Venue? = venueDao.getById(id)
    suspend fun addVenue(venue: Venue): Long = venueDao.insert(venue)
    suspend fun deleteVenue(id: Long) = venueDao.delete(id)

    // AttendanceType
    fun getAllAttendanceTypes(): Flow<List<AttendanceType>> = attendanceTypeDao.getAll()
    suspend fun getAttendanceTypeById(id: Long): AttendanceType? = attendanceTypeDao.getById(id)
    suspend fun addAttendanceType(type: AttendanceType): Long = attendanceTypeDao.insert(type)
    suspend fun deleteAttendanceType(id: Long) = attendanceTypeDao.delete(id)

    // Inbound
    fun getAllInboundRecords(): Flow<List<InboundRecord>> = inboundDao.getAll()
    suspend fun getTotalInboundCount(): Int = inboundDao.getTotalCount()
    suspend fun getOperationDays(): Int = inboundDao.getOperationDays()
    suspend fun getInboundByMonth(yearMonth: String): InboundRecord? =
        inboundDao.getByMonth(yearMonth)
    suspend fun saveInboundRecord(record: InboundRecord): Long =
        inboundDao.insert(record)
}
