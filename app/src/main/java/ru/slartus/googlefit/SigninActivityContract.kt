package ru.slartus.googlefit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.fitness.Fitness

class GoogleFitSignInActivityContract(context: Context, clientId: String) :
    ActivityResultContract<Any, String?>() {
    private val googleSignInClient: GoogleSignInClient = createSignInClient(context, clientId)
    override fun createIntent(context: Context, input: Any?): Intent {
        val googleSignInClient = googleSignInClient

        return googleSignInClient.signInIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? = when {
        resultCode != Activity.RESULT_OK -> {
            val extras = intent?.extras
            if (extras?.containsKey("googleSignInStatus") == true) {
                try {
                    intent.getParcelableExtra<Status>("googleSignInStatus")?.let { status ->
                        when (status.statusCode) {
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                                // ignore: cancelled by user
                            }
                            GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                                // check developer console!
                                Timber.e("Google sign in status ${status.statusCode}: The application is misconfigured")
                            }
                            else -> {
                                Timber.e("Google sign in status ${status.statusCode} with message: ${status.statusMessage} ")
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    Timber.w(ex)
                }
            } else {
                Timber.i("google sign in resultCode: $resultCode")
            }
            null
        }
        intent == null -> {
            Timber.w("google sign intent is null")

            null
        }
        else -> {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent)
            result?.signInAccount?.serverAuthCode
        }
    }


    override fun getSynchronousResult(
        context: Context,
        input: Any?
    ): SynchronousResult<String?>? {
        return null
    }

    companion object {
        private const val SCOPE_HEART_RATE_READ =
            "https://www.googleapis.com/auth/fitness.heart_rate.read"

        fun silentSignInAuthCode(
            context: Context,
            clientId: String,
            onComplete: (serverAuthCode: String?) -> Unit
        ) {
            val googleFitClient = createSignInClient(context, clientId)
            googleFitClient.silentSignIn().addOnCompleteListener {
                onComplete(it.result?.serverAuthCode)
            }
        }

        fun createSignInClient(context: Context, clientId: String): GoogleSignInClient {

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(
                    Fitness.SCOPE_ACTIVITY_READ, // calories
                    Fitness.SCOPE_LOCATION_READ, // distance
                    Scope(SCOPE_HEART_RATE_READ)
                )
                .requestId()
                .requestServerAuthCode(clientId)
                .build()
            return GoogleSignIn.getClient(context, gso)
        }
    }
}