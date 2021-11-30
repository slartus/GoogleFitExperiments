package ru.slartus.googlefit

import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

val types: List<DataType> by lazy {
    listOf(
        DataType.TYPE_ACTIVITY_SEGMENT,
        DataType.TYPE_HEART_RATE_BPM,
        DataType.TYPE_CALORIES_EXPENDED,
        DataType.TYPE_DISTANCE_DELTA,
        DataType.TYPE_STEP_COUNT_CUMULATIVE,
        DataType.TYPE_WEIGHT,
        DataType.TYPE_HEIGHT,
        DataType.TYPE_CYCLING_PEDALING_CADENCE,
        DataType.TYPE_CYCLING_PEDALING_CUMULATIVE,
        DataType.TYPE_BASAL_METABOLIC_RATE,
        DataType.TYPE_BODY_FAT_PERCENTAGE,
        DataType.TYPE_LOCATION_SAMPLE,
    )
}
val fitnessOptions: FitnessOptions by lazy {
    val builder = FitnessOptions.builder()
    types.forEach {
        builder.addDataType(it, FitnessOptions.ACCESS_READ)
    }
    builder
        .build()
}

val readRequest: DataReadRequest by lazy {
    val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
    val startTime = endTime.minusWeeks(5)
    val builder = DataReadRequest.Builder()
    types.forEach {
        builder.read(it)
    }
    builder
        .bucketBySession(1, TimeUnit.MINUTES)
        .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
        .build()
}