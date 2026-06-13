package com.worklog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.worklog.app.data.dao.*
import com.worklog.app.data.entity.*

@Database(
    entities = [
        Employee::class,
        DutyRecord::class,
        AttendanceRecord::class,
        BookingRecord::class,
        Venue::class,
        AttendanceType::class,
        InboundRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun dutyDao(): DutyDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun bookingDao(): BookingDao
    abstract fun venueDao(): VenueDao
    abstract fun attendanceTypeDao(): AttendanceTypeDao
    abstract fun inboundDao(): InboundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worklog_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
