import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.demomynumber.PersonalInfo

@Composable
fun PersonalInfoScreen(
    personalInfo: PersonalInfo?,
    onRequestInfo: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("マイナンバーカードからの個人情報", style = MaterialTheme.typography.titleLarge)

        Button(onClick = onRequestInfo) {
            Text("情報を取得")
        }

        personalInfo?.let {
            HorizontalDivider()
            Text("👤 氏名: ${it.name}")
            HorizontalDivider()
            Text("🏠 住所: ${it.address}")
            HorizontalDivider()
            Text("📅 生年月日: ${it.birth}")
            HorizontalDivider()
            Text("⚧️ 性別: ${genderToText(it.gender)}")
        }
    }
}

fun genderToText(code: String) = when (code) {
    "1" -> "男性"
    "2" -> "女性"
    else -> "不明"
}