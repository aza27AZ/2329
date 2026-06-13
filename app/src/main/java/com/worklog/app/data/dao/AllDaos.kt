package com.worklog.app.data.dao

import androidx.room.*
import com.worklog.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY isActive DESC, name ASC")
    fun getAll(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun getActive(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): Employee?

    @Insert
    suspend fun insert(employee: Employee): Long

    @Update
    suspend fun update(employee: Employee)

    @Query("UPDATE employees SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE employees SET isActive = 1 WHERE id = :id")
    suspend fun restore(id: Long)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_records WHERE date >= :startDate AND date <= :endDate")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<DutyRecord>>

    @Query("SELECT * FROM duty_records WHERE date = :date")
    suspend fun getByDate(date: String): List<DutyRecord>

    @Query("SELECT * FROM duty_records WHERE employeeId = :employeeId AND date >= :startDate AND date <= :endDate")
    suspend fun getByEmployee(employeeId: Long, startDate: String, endDate: String): List<DutyRecord>

    @Query("SELECT * FROM duty_records WHERE employeeId = :employeeId AND date = :date")
    suspend fun getByEmployeeAndDate(employeeId: Long, date: String): DutyRecord?

    @Insert
    suspend fun insert(duty: DutyRecord): Long

    @Insert
    suspend fun insertAll(duties: List<DutyRecord>)

    @Delete
    suspend fun delete(duty: DutyRecord)

    @Query("DELETE FROM duty_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT shiftType, COUNT(*) as count 
        FROM duty_records 
        WHERE employeeId = :employeeId AND date >= :startDate AND date <= :endDate 
        GROUP BY shiftType
    """)
    suspend fun getShiftCounts(employeeId: Long, startDate: String, endDate: String): List<ShiftCount>

    data class ShiftCount(val shiftType: String, val count: Int)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY startDate DESC, id DESC")
    fun getAll(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE typeId = :typeId ORDER BY startDate DESC, id DESC")
    fun getByType(typeId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date(startDate) >= :start AND date(startDate) <= :end ORDER BY startDate DESC")
    fun getByDateRange(start: String, end: String): Flow<List<AttendanceRecord>>

    @Insert
    suspend fun insert(record: AttendanceRecord): Long

    @Update
    suspend fun update(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM booking_records ORDER BY date DESC, startTime DESC")
    fun getAll(): Flow<List<BookingRecord>>

    @Query("SELECT * FROM booking_records WHERE status = :status ORDER BY date DESC, startTime DESC")
    fun getByStatus(status: String): Flow<List<BookingRecord>>

    @Query("SELECT * FROM booking_records WHERE date = :date")
    suspend fun getByDate(date: String): List<BookingRecord>

    @Query("""
        SELECT * FROM booking_records 
        WHERE venueId = :venueId AND date = :date AND status != '已取消'
        ORDER BY startTime ASC
    """)
    suspend fun getByVenueAndDate(venueId: Long, date: String): List<BookingRecord>

    @Insert
    suspend fun insert(record: BookingRecord): Long

    @Update
    suspend fun update(record: BookingRecord)

    @Delete
    suspend fun delete(record: BookingRecord)

    @Query("DELETE FROM booking_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface VenueDao {
    @Query("SELECT * FROM venues ORDER BY name ASC")
    fun getAll(): Flow<List<Venue>>

    @Query("SELECT * FROM venues WHERE id = :id")
    suspend fun getById(id: Long): Venue?

    @Insert
    suspend fun insert(venue: Venue): Long

    @Query("DELETE FROM venues WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AttendanceTypeDao {
    @Query("SELECT * FROM attendance_types ORDER BY id ASC")
    fun getAll(): Flow<List<AttendanceType>>

    @Query("SELECT * FROM attendance_types WHERE id = :id")
    suspend fun getById(id: Long): AttendanceType?

    @Insert
    suspend fun insert(type: AttendanceType): Long

    @Query("DELETE FROM attendance_types WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface InboundDao {
    @Query("SELECT * FROM inbound_records ORDER BY yearMonth DESC")
    fun getAll(): Flow<List<InboundRecord>>

    @Query("SELECT SUM(count) FROM inbound_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM inbound_records")
    suspend fun getOperationDays(): Int

    @Query("SELECT * FROM inbound_records WHERE yearMonth = :yearMonth")
    suspend fun getByMonth(yearMonth: String): InboundRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InboundRecord): Long
}
