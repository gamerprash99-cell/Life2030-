package com.lifeos.app.data.db

import androidx.room.TypeConverter
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.HabitOutcome
import com.lifeos.app.data.db.entities.HabitScheduleType
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.PaymentMethod
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.TaskPriority

class Converters {
    @TypeConverter fun fromPriority(value: TaskPriority): String = value.name
    @TypeConverter fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter fun fromRepeatRule(value: RepeatRule): String = value.name
    @TypeConverter fun toRepeatRule(value: String): RepeatRule = RepeatRule.valueOf(value)

    @TypeConverter fun fromFrequency(value: HabitFrequency): String = value.name
    @TypeConverter fun toFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @TypeConverter fun fromHabitType(value: HabitType): String = value.name
    @TypeConverter fun toHabitType(value: String): HabitType = HabitType.valueOf(value)

    @TypeConverter fun fromTrackingMode(value: HabitTrackingMode): String = value.name
    @TypeConverter fun toTrackingMode(value: String): HabitTrackingMode = HabitTrackingMode.valueOf(value)

    @TypeConverter fun fromScheduleType(value: HabitScheduleType): String = value.name
    @TypeConverter fun toScheduleType(value: String): HabitScheduleType = HabitScheduleType.valueOf(value)

    @TypeConverter fun fromHabitOutcome(value: HabitOutcome): String = value.name
    @TypeConverter fun toHabitOutcome(value: String): HabitOutcome = HabitOutcome.valueOf(value)

    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}
