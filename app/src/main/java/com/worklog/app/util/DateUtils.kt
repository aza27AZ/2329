package com.worklog.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val displayFormatter = DateTimeFormatter.ofPattern("MM月dd日")

    fun today(): String = LocalDate.now().format(dateFormatter)
    fun currentYearMonth(): String = LocalDate.now().format(monthFormatter)

    fun formatDisplay(dateStr: String): String {
        return try {
            LocalDate.parse(dateStr, dateFormatter).format(displayFormatter)
        } catch (e: Exception) { dateStr }
    }

    fun formatDisplayFull(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            "${date.format(displayFormatter)} 星期${weekDays[date.dayOfWeek.value % 7]}"
        } catch (e: Exception) { dateStr }
    }

    fun getMonthStart(yearMonth: String): String = "$yearMonth-01"

    fun getMonthEnd(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt())
        return ym.atEndOfMonth().format(dateFormatter)
    }

    fun getWeekStart(date: LocalDate = LocalDate.now()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun getWeekEnd(date: LocalDate = LocalDate.now()): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun getMonthDays(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        return YearMonth.of(parts[0].toInt(), parts[1].toInt()).lengthOfMonth()
    }

    fun getFirstDayOfWeek(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        return date.dayOfWeek.value % 7
    }

    fun parseDate(dateStr: String): LocalDate? {
        return try { LocalDate.parse(dateStr, dateFormatter) } catch (e: Exception) { null }
    }

    fun getDateList(yearMonth: String): List<String> {
        val parts = yearMonth.split("-")
        val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt())
        return (1..ym.lengthOfMonth()).map {
            "$yearMonth-${it.toString().padStart(2, '0')}"
        }
    }

    fun previousMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt()).minusMonths(1)
        return ym.format(monthFormatter)
    }

    fun nextMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val ym = YearMonth.of(parts[0].toInt(), parts[1].toInt()).plusMonths(1)
        return ym.format(monthFormatter)
    }

    fun getWeekNumberInMonth(date: LocalDate): Int {
        val firstDay = date.withDayOfMonth(1)
        val firstMonday = firstDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        if (date.isBefore(firstMonday)) return 0
        return ((date.toEpochDay() - firstMonday.toEpochDay()) / 7).toInt() + 1
    }
}
