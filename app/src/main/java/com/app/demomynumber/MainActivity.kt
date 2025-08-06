package com.app.demomynumber

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val defaultResultText = stringResource(R.string.default_result)
    val errorNoDataText = stringResource(R.string.error_no_data)
    val errorGetInfoTextFormat = stringResource(R.string.error_get_info)
    val successInfoTextFormat = stringResource(R.string.success_info)
    val errorNoCertDataText = stringResource(R.string.error_no_cert_data)
    val errorGetCertTextFormat = stringResource(R.string.error_get_cert)
    val errorInitFailedTextFormat = stringResource(R.string.error_init_failed)
    val processingInitText = stringResource(R.string.processing_init)
    val buttonText = stringResource(R.string.button_text)
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
        Log.d("JPKI", "getBasicInfoLauncher: resultCode=${result.resultCode}, data=$data")
        Toast.makeText(context, "Lấy thông tin cơ bản: resultCode=${result.resultCode}, data=$data", Toast.LENGTH_SHORT).show()
        if (data == null) {
            resultText = errorNoDataText
            Log.e("JPKI", "getBasicInfoLauncher: No data received")
            return@rememberLauncherForActivityResult
        }

        val err = data.getIntExtra("err_code", -1)
        val detail = data.getIntExtra("detail_code", -1)
        Log.d("JPKI", "getBasicInfoLauncher: err_code=$err, detail_code=$detail")
        if (err == 0) {
            val name = data.getStringExtra("name").orEmpty()
            val address = data.getStringExtra("address").orEmpty()
            val gender = data.getStringExtra("gender").orEmpty()
            val birthdate = data.getStringExtra("date_of_birth").orEmpty()
            Log.d("JPKI", "Basic Info: name=$name, address=$address, gender=$gender, birthdate=$birthdate")
            resultText = successInfoTextFormat.format(name, address, gender, birthdate)
        } else {
            resultText = errorGetInfoTextFormat.format(err, detail)
            Log.e("JPKI", "getBasicInfoLauncher: Failed to get info")
        }
        Toast.makeText(context, "Lấy thông tin cơ bản: err=$err, detail=$detail", Toast.LENGTH_SHORT).show()
        finishSessionLauncher.launch(getJpkiIntent(0x01004002))
    }

    val getCertLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        Log.d("JPKI", "getCertLauncher: resultCode=${result.resultCode}, data=$data")
        Toast.makeText(context, "getCertLauncher: resultCode=${result.resultCode}, data=$data", Toast.LENGTH_SHORT).show()
        if (data == null) {
            resultText = errorNoCertDataText
            Log.e("JPKI", "getCertLauncher: No data received")
            processing = false
            return@rememberLauncherForActivityResult
        }

        val err = data.getIntExtra("err_code", -1)
        val detail = data.getIntExtra("detail_code", -1)
        val cert = data.getByteArrayExtra("p_cert")
        Log.d("JPKI", "getCertLauncher: err_code=$err, detail_code=$detail, cert=${cert?.size ?: "null"} bytes")
        Toast.makeText(context, "Lấy chứng chỉ: err=$err, detail=$detail", Toast.LENGTH_SHORT).show()
        if (cert != null) {
            val intent = getExtractInfoIntent(cert)
            Log.d("JPKI", "Launching getBasicInfo with cert")
            getBasicInfoLauncher.launch(intent)
        } else {
            resultText = errorGetCertTextFormat.format(err, detail)
            Log.e("JPKI", "getCertLauncher: cert is null")
            processing = false
        }
    }

    val initLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        Log.d("JPKI", "initLauncher: resultCode=${result.resultCode}, data=$data")

        if (data == null) {
            resultText = errorNoDataText
            Log.e("JPKI", "initLauncher: No data received")
            processing = false
            return@rememberLauncherForActivityResult
        }

        val ok = data.getBooleanExtra("result", false)
        val err = data.getIntExtra("err_code", -1)
        val detail = data.getIntExtra("detail_code", -1)
        Log.d("JPKI", "initLauncher: result=$ok, err_code=$err, detail_code=$detail")
        Toast.makeText(context, "Khởi tạo: err=$err, detail=$detail", Toast.LENGTH_SHORT).show()
        if (ok) {
            Log.d("JPKI", "Initialization successful, launching getCert")
            getCertLauncher.launch(getJpkiIntent(0x01002002))
        } else {
            resultText = errorInitFailedTextFormat.format(err, detail)
            Log.e("JPKI", "initLauncher: Initialization failed")
            processing = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.title_result, "1.3"), style = MaterialTheme.typography.titleMedium)
        Text(resultText, style = MaterialTheme.typography.bodyMedium)

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
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
