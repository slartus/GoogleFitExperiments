package ru.slartus.googlefit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness

fun createSignInClient(context: Context): GoogleSignInClient {
    val scopeHearRate = "https://www.googleapis.com/auth/fitness.heart_rate.read"
    val serverClientId = context.getString(R.string.server_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(
            Fitness.SCOPE_ACTIVITY_READ, // calories
            Fitness.SCOPE_LOCATION_READ, // distance
            Scope(scopeHearRate)
        )
        .requestServerAuthCode(serverClientId)
        .build()
    return GoogleSignIn.getClient(context, gso)
}

class SignInActivityContract : ActivityResultContract<Any, GoogleSignInResult?>() {
    override fun createIntent(context: Context, input: Any?): Intent {
        val googleSignInClient = createSignInClient(context)
        return googleSignInClient.signInIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GoogleSignInResult? = when {
        resultCode != Activity.RESULT_OK -> null
        intent == null -> null
        else -> Auth.GoogleSignInApi.getSignInResultFromIntent(intent)
    }

    override fun getSynchronousResult(
        context: Context,
        input: Any?
    ): SynchronousResult<GoogleSignInResult?>? {
        return null
    }
}