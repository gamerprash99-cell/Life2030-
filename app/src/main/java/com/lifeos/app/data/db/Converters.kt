package com.lifeos.app.data.db

import androidx.room.TypeConverter
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.db.entities.HabitFrequency
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

    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter fun fromCaptureType(value: CaptureType): String = value.name
    @TypeConverter fun toCaptureType(value: String): CaptureType = CaptureType.valueOf(value)
}
