package ru.slartus.googlefit

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.data.DataPoint
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class FitActionRequestCode {
    READ_DATA
}

val runningQOrLater =
    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

fun Context.getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
fun Context.oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)


fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime().toString()

fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime().toString()
