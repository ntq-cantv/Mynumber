package com.app.demomynumber

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JpkiBasicInfoScreen()
            }
        }
    }
}

@Composable
fun JpkiBasicInfoScreen() {
    val defaultResultText = stringResource(R.string.default_result)
    val errorNoDataText = stringResource(R.string.error_no_data)
    val errorGetInfoTextFormat = stringResource(R.string.error_get_info)
    val successInfoTextFormat = stringResource(R.string.success_info)
    val errorNoCertDataText = stringResource(R.string.error_no_cert_data)
    val errorCertNullText = stringResource(R.string.error_cert_null)
    val errorGetCertTextFormat = stringResource(R.string.error_get_cert)
    val errorInitFailedTextFormat = stringResource(R.string.error_init_failed)
    val processingInitText = stringResource(R.string.processing_init)
    val buttonText = stringResource(R.string.button_text)
    val titleResultText = stringResource(R.string.title_result)

    var resultText by remember { mutableStateOf(defaultResultText) }
    var processing by remember { mutableStateOf(false) }

    val finishSessionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {}

    val getBasicInfoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        processing = false
        val data = result.data
        if (data == null) {
            resultText = errorNoDataText
            return@rememberLauncherForActivityResult
        }

        val ok = data.getBooleanExtra("result", false)
        if (ok) {
            val name = data.getStringExtra("name")
            val address = data.getStringExtra("address")
            val gender = data.getStringExtra("gender")
            val birthdate = data.getStringExtra("date_of_birth")

            resultText = successInfoTextFormat.format(name, address, gender, birthdate)
        } else {
            val err = data.getIntExtra("err_code", -1)
            val detail = data.getIntExtra("detail_code", -1)
            resultText = errorGetInfoTextFormat.format(err, detail)
        }

        finishSessionLauncher.launch(getJpkiIntent(0x01004002))
    }

    val getCertLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            resultText = errorNoCertDataText
            processing = false
            return@rememberLauncherForActivityResult
        }
        val errCode = data.getIntExtra("err_code", -1)
        if (errCode == 0) {
            // Thành công
            val name = data.getStringExtra("name")
            val address = data.getStringExtra("address")
            val gender = data.getStringExtra("gender")
            val birthdate = data.getStringExtra("date_of_birth")

            resultText = successInfoTextFormat.format(name, address, gender, birthdate)
        } else {
            val detailCode = data.getIntExtra("detail_code", -1)
            resultText = errorGetInfoTextFormat.format(errCode, detailCode)
        }

    }

    val initLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            resultText = errorNoDataText
            processing = false
            return@rememberLauncherForActivityResult
        }

        val ok = data.getBooleanExtra("result", false)
        if (ok) {
            getCertLauncher.launch(getJpkiIntent(0x01002002))
        } else {
            val err = data.getIntExtra("err_code", -1)
            val detail = data.getIntExtra("detail_code", -1)
            resultText = errorInitFailedTextFormat.format(err, detail)
            processing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(titleResultText, style = MaterialTheme.typography.titleMedium)
        Text(resultText, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = {
                processing = true
                resultText = processingInitText
                initLauncher.launch(getJpkiIntent(0x01004001))
            },
            enabled = !processing,
        ) {
            Text(buttonText)
        }
    }
}

fun getJpkiIntent(commandType: Int): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        setClassName(
            "jp.go.jpki.mobile.utility",
            "jp.go.jpki.mobile.intent.JPKIIntentActivity",
        )
        putExtra("command_type", commandType)
    }
}

fun getExtractInfoIntent(cert: ByteArray): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        setClassName(
            "jp.go.jpki.mobile.utility",
            "jp.go.jpki.mobile.intent.JPKIIntentActivity",
        )
        putExtra("command_type", 0x01003002)
        putExtra("cert", cert)
    }
}
