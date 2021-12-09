package ru.slartus.googlefit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.Field
import com.google.android.material.snackbar.Snackbar
import com.squareup.okhttp.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    //https://stackoverflow.com/questions/33998335/how-to-get-access-token-after-user-is-signed-in-from-gmail-in-android
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()){ isGranted ->
            // Handle Permission granted/rejected
            if (isGranted) {
                // Permission is granted
            } else {
                // Permission is denied
            }
        }

    private val signInResultLauncher = registerForActivityResult(
        SignInActivityContract()
    ) { result ->
        if (result != null) {
            onAuthCodeRequestCallback(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //checkPermissionsAndRun(FitActionRequestCode.READ_DATA)
        val googleSignInClient = createSignInClient(this)
        googleSignInClient.silentSignIn().addOnCompleteListener {
            if (it.isSuccessful) {
                requestToken(it.result)
            } else {
                requestTokens()
            }
        }
    }

    private fun requestTokens() {
        signInResultLauncher.launch(null)
    }

    private fun accessGoogleFit() {
        val account = getGoogleAccount()
        Log.d(TAG, "account id: ${account.id}")

        val client = Fitness.getHistoryClient(this, account)

        client
            .readData(readRequest)
            .addOnSuccessListener { response ->
                for (bucket in response.buckets) {
                    dumpBucket(bucket)
                }
                for (dataSet in response.dataSets) {
                    dumpDataSet(dataSet)
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "OnFailure()", e)
            }
    }

    private fun dumpBucket(bucket: Bucket) {
        Log.i(TAG, "session id: ${bucket.session?.identifier ?: "no id"}")
        for (dataSet in bucket.dataSets) {
            dumpDataSet(dataSet)
        }
    }

    private fun dumpDataSet(dataSet: DataSet) {
        for (dp in dataSet.dataPoints) {
            dumpDataPoint(dp)
        }
    }

    private fun dumpDataPoint(dp: DataPoint) {
        Log.i(
            TAG,
            "\t\t${dp.dataType.name} ${dp.getStartTimeString()} - ${dp.getEndTimeString()}"
        )
        for (field in dp.dataType.fields) {
            if (field == Field.FIELD_ACTIVITY)
                Log.i(TAG, "\t\t\t${field.name}: ${dp.getValue(field).asActivity()}")
            else
                Log.i(TAG, "\t\t\t${field.name}: ${dp.getValue(field)}")
        }
    }

//    unknown
//    dcc8713a-ef3a-41ff-9b94-ce2fbd5c5382
//      com.google.activity.segment 2021-11-28T08:52:12 - 2021-11-28T09:22:12
//      activity Value: rowing
//      com.google.calories.expended 2021-11-28T08:52:12 - 2021-11-28T09:22:12
//      calories Value: 111.0
//      com.google.distance.delta 2021-11-28T08:52:12 - 2021-11-28T09:22:12
//      distance Value: 4400.0
//    unknown
//      013b148f-03dd-4f37-a069-b4c8b95a74e9
//    unknown
//      1638243720284-1638249120284
//      com.google.activity.segment 2021-11-30T08:42 - 2021-11-30T10:12
//      activity Value: running
//      com.google.calories.expended 2021-11-30T08:42 - 2021-11-30T10:12
//      calories Value: 1215.0
//      com.google.distance.delta 2021-11-30T08:42 - 2021-11-30T10:12
//      distance Value: 4000.0
//      com.google.step_count.cumulative 2021-11-21T02:01:54 - 2021-11-30T09:11:38
//      steps Value: 0
//    unknown
//      50367854-8fa9-423c-a78e-60ec84b099df
//    unknown
//      98f08b05-073f-4c2f-b488-9aab01bb8a74

    private fun checkPermissionsAndRun(requestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(requestCode)
        } else {
            requestRuntimePermissions(requestCode)
        }
    }

    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            accessGoogleFit()
        } else {
            GoogleSignIn.requestPermissions(
                this,
                requestCode.ordinal,
                getGoogleAccount(), fitnessOptions
            )
        }
    }

    private fun requestToken(googleAccount: GoogleSignInAccount) {
        val client = OkHttpClient()
        val requestBody = FormEncodingBuilder()
            .add("grant_type", "authorization_code")
            .add(
                "client_id",
                getString(R.string.server_client_id)
            )
            .add("client_secret", "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")// from
            .add("redirect_uri", "")
            .add("code", googleAccount.serverAuthCode)
            .build()
        val request: Request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v4/token")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException) {
                Log.e(TAG, e.toString())
            }

            @Throws(IOException::class)
            override fun onResponse(response: Response) {
                try {
                    val jsonObject = JSONObject(response.body().string())
                    val message: String = jsonObject.toString(5)
                    val token = jsonObject.getString("access_token")
                    Log.i(TAG, token)
                    Log.i(TAG, message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun onAuthCodeRequestCallback(result: GoogleSignInResult) {
        if (result.isSuccess) {
            //accessGoogleFit()
            // [START get_auth_code]
            val acct = result.signInAccount
            requestToken(acct)
        } else {
            // Show signed-out UI.

        }
    }

    /**
     * Handles the callback from the OAuth sign in flow, executing the post sign in function
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            Activity.RESULT_OK ->
                when (requestCode) {
                    FitActionRequestCode.READ_DATA.ordinal -> accessGoogleFit()
                    else -> {
                        // Result wasn't from Google Fit
                    }
                }
            else -> {
                // Permission not granted
            }
        }
    }

    private fun permissionApproved(): Boolean {
        val approved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
        return approved
    }

    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.")
                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.ok) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                            requestCode.ordinal
                        )
                    }
                    .show()
            } else {
                Log.i(TAG, "Requesting permission")
                // Request permission. It's possible this can be auto answered if device policy
                // sets the permission in a given state or the user denied the permission
                // previously and checked "Never ask again".
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    requestCode.ordinal
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            grantResults.isEmpty() -> {
                // If user interaction was interrupted, the permission request
                // is cancelled and you receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            }
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                // Permission was granted.
                val fitActionRequestCode = FitActionRequestCode.values()[requestCode]
                fitActionRequestCode.let {
                    fitSignIn(fitActionRequestCode)
                }
            }
            else -> {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.

                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e(TAG, p0.errorMessage)
    }
}