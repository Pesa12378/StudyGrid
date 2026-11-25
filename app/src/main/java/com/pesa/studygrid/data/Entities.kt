package com.pesa.studygrid.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Intensity levels are fixed: 0 = nothing, 1 = Bajo, 2 = Medio, 3 = Alto, 4 = Intenso. */
const val MAX_LEVEL = 4

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** ARGB color. */
    val color: Int,
    /** Target active days per week (1..7); 0 = no weekly goal. Added in DB version 2. */
    @ColumnInfo(defaultValue = "0") val weeklyTarget: Int = 0,
)

@Entity(
    tableName = "daily_logs",
    primaryKeys = ["goalId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DailyLogEntity(
    val goalId: Long,
    /** LocalDate.toEpochDay(), so no type converter is needed. */
    val epochDay: Long,
    val intensity: Int,
    val note: String = "",
)
