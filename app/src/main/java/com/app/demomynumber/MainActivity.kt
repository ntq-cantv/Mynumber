package com.app.demomynumber

import PersonalInfoScreen
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val personalInfoState = mutableStateOf<PersonalInfo?>(null)

    private val getPersonalInfoLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "getPersonalInfoLauncher resultCode: ${result.resultCode}, data: ${result.data}")
            logToFile("getPersonalInfoLauncher resultCode: ${result.resultCode}, data: ${result.data}")
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                val info = PersonalInfo(
                    name = data?.getStringExtra("name") ?: "",
                    address = data?.getStringExtra("address") ?: "",
                    gender = data?.getStringExtra("gender") ?: "",
                    birth = data?.getStringExtra("date_of_birth") ?: ""
                )
                Log.d(TAG, "Personal info received: $info")
                logToFile("Personal info received: $info")
                personalInfoState.value = info
            } else {
                Log.e(TAG, "Failed to get personal info or user cancelled. Result code: ${result.resultCode}")
                logToFile("Failed to get personal info or user cancelled. Result code: ${result.resultCode}")
                showErrorToast("個人情報を取得できなかったか、ユーザーがキャンセルしました。結果コード: ${result.resultCode}")
            }
        }

    private val getCertificateLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "getCertificateLauncher resultCode: ${result.resultCode}, data: ${result.data}")
            logToFile("getCertificateLauncher resultCode: ${result.resultCode}, data: ${result.data}")
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                val errCode = result.data?.getIntExtra("err_code", -1)
                val detailCode = result.data?.getIntExtra("detail_code", -1)
                logToFile("Lỗi đọc thẻ: err_code = $errCode, detail_code = $detailCode")
                val certBytes = data?.getByteArrayExtra("p_cert")
                if (certBytes != null) {
                    Log.d(TAG, "Certificate bytes received, launching getPersonalInfoLauncher")
                    logToFile("Certificate bytes received, launching getPersonalInfoLauncher")
                    val infoIntent = Intent(Intent.ACTION_SEND).apply {
                        setClassName("jp.go.jpki.mobile.utility", "jp.go.jpki.mobile.intent.JPKIIntentActivity")
                        putExtra("command_type", 0x01003002)
                        putExtra("cert", certBytes)
                    }
                    getPersonalInfoLauncher.launch(infoIntent)
                } else {
                    Log.e(TAG, "Certificate bytes are null.")
                    logToFile("Certificate bytes are null.")
                    showErrorToast("証明書を取得できませんでした。")
                }
            } else {
                Log.e(TAG, "Failed to get certificate or user cancelled. Result code: ${result.resultCode}")
                logToFile("Failed to get certificate or user cancelled. Result code: ${result.resultCode}")
                showErrorToast("証明書を取得できなかったか、ユーザーがキャンセルしました。結果コード: ${result.resultCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        logToFile("onCreate called")
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PersonalInfoScreen(
                        personalInfo = personalInfoState.value,
                        onRequestInfo = { launchGetCertIntent() }
                    )
                }
            }
        }
    }

    private fun launchGetCertIntent() {
        Log.d(TAG, "Launching certificate intent")
        logToFile("Launching certificate intent")
        val certIntent = Intent(Intent.ACTION_SEND).apply {
            setClassName("jp.go.jpki.mobile.utility", "jp.go.jpki.mobile.intent.JPKIIntentActivity")
            putExtra("command_type", 0x01001002)
        }
        getCertificateLauncher.launch(certIntent)
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun logToFile(message: String) {
        val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null) {
            val logFile = File(downloadsDir, "app_log.txt")
            try {
                FileWriter(logFile, true).use { writer ->
                    writer.appendLine("${System.currentTimeMillis()}: $message")
                }
                Log.d("MainActivity", "Log written to: ${logFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("MainActivity", "Failed to write log", e)
            }
        } else {
            Log.e("MainActivity", "downloadsDir is null")
        }
    }

}

data class PersonalInfo(
    val name: String,
    val address: String,
    val gender: String,
    val birth: String
)