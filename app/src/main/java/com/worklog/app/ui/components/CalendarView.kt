package com.worklog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.worklog.app.data.entity.DutyRecord
import com.worklog.app.ui.theme.Blue
import com.worklog.app.ui.theme.DutyEvening
import com.worklog.app.ui.theme.DutyFull
import com.worklog.app.ui.theme.DutyMorning
import com.worklog.app.util.DateUtils
import java.time.YearMonth

@Composable
fun MonthGrid(
    yearMonth: String,
    dutyData: Map<String, List<DutyRecord>>,
    onDayClick: (String) -> Unit
) {
    var currentYearMonth by remember { mutableStateOf(yearMonth) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CalendarHeader(
            yearMonth = currentYearMonth,
            onPrevious = { currentYearMonth = DateUtils.previousMonth(currentYearMonth) },
            onNext = { currentYearMonth = DateUtils.nextMonth(currentYearMonth) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        WeekDayHeader()

        Spacer(modifier = Modifier.height(4.dp))

        CalendarGrid(
            yearMonth = currentYearMonth,
            dutyData = dutyData,
            onDayClick = onDayClick
        )
    }
}

@Composable
private fun CalendarHeader(
    yearMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val parts = yearMonth.split("-")
    val year = parts[0].toIntOrNull() ?: 2026
    val month = parts[1].toIntOrNull() ?: 1
    val displayText = "${year}年${month}月"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上个月",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = displayText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下个月",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Text(
                text = day,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: String,
    dutyData: Map<String, List<DutyRecord>>,
    onDayClick: (String) -> Unit
) {
    val parts = yearMonth.split("-")
    val year = parts[0].toIntOrNull() ?: return
    val month = parts[1].toIntOrNull() ?: return

    val yearMonthObj = YearMonth.of(year, month)
    val firstDayOfWeek = DateUtils.getFirstDayOfWeek(yearMonth)
    val daysInMonth = yearMonthObj.lengthOfMonth()
    val prevMonth = yearMonthObj.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()

    val todayStr = DateUtils.today()

    val totalCells = daysInMonth + firstDayOfWeek
    val rows = (totalCells + 6) / 7
    val totalSlots = rows * 7

    val cells = mutableListOf<CalendarCell>()

    for (i in 0 until firstDayOfWeek) {
        val day = daysInPrevMonth - firstDayOfWeek + i + 1
        val dateStr = "${prevMonth.year}-${prevMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        cells.add(CalendarCell(day = day, dateStr = dateStr, isCurrentMonth = false))
    }

    for (day in 1..daysInMonth) {
        val dateStr = "$yearMonth-${day.toString().padStart(2, '0')}"
        cells.add(CalendarCell(day = day, dateStr = dateStr, isCurrentMonth = true))
    }

    for (i in (daysInMonth + firstDayOfWeek) until totalSlots) {
        val day = i - daysInMonth - firstDayOfWeek + 1
        val nextMonth = yearMonthObj.plusMonths(1)
        val dateStr = "${nextMonth.year}-${nextMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        cells.add(CalendarCell(day = day, dateStr = dateStr, isCurrentMonth = false))
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    if (index < cells.size) {
                        val cell = cells[index]
                        val isToday = cell.dateStr == todayStr
                        val dayRecords = dutyData[cell.dateStr]

                        DayCell(
                            day = cell.day,
                            isCurrentMonth = cell.isCurrentMonth,
                            isToday = isToday,
                            records = dayRecords,
                            onClick = { onDayClick(cell.dateStr) }
                        )
                    }
                }
            }
        }
    }
}

private data class CalendarCell(
    val day: Int,
    val dateStr: String,
    val isCurrentMonth: Boolean
)

@Composable
private fun DayCell(
    day: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    records: List<DutyRecord>?,
    onClick: () -> Unit
) {
    val textColor = if (isCurrentMonth) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val borderModifier = if (isToday) {
        Modifier.border(
            width = 1.5.dp,
            color = Blue,
            shape = CircleShape
        )
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(40.dp)
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = borderModifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (isToday) Modifier else Modifier.background(Color.Transparent)
                )
        ) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Blue else textColor,
                textAlign = TextAlign.Center
            )
        }

        if (!records.isNullOrEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(6.dp)
            ) {
                items(records.take(3)) { record ->
                    val dotColor = when (record.shiftType) {
                        "早班" -> DutyMorning
                        "晚班" -> DutyEvening
                        "通班" -> DutyFull
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
