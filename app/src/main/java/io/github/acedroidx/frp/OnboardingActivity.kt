package io.github.acedroidx.frp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.acedroidx.frp.ui.theme.FrpTheme
import io.github.acedroidx.frp.ui.theme.ThemeModeKeys
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import android.util.Log

class OnboardingActivity : ComponentActivity() {
    private val themeMode = MutableStateFlow(ThemeModeKeys.FOLLOW_SYSTEM)
    private val notificationPermissionGranted = MutableStateFlow(true)
    private val ignoringBatteryOptimizations = MutableStateFlow(false)

    private lateinit var preferences: SharedPreferences

    // 通知权限请求启动器
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted.value = granted
    }

    // 电池优化豁免申请
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 回到应用后更新当前状态，避免用户手动取消时状态错误
        updateBatteryOptimizationStatus()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences("data", MODE_PRIVATE)

        val rawTheme = preferences.getString(PreferencesKey.THEME_MODE, ThemeModeKeys.FOLLOW_SYSTEM)
        themeMode.value = ThemeModeKeys.normalize(rawTheme)

        updateNotificationPermissionStatus()
        updateBatteryOptimizationStatus()

        enableEdgeToEdge()
        setContent {
            val currentTheme by themeMode.collectAsStateWithLifecycle(themeMode.collectAsState().value)
            val notificationGranted by notificationPermissionGranted.collectAsStateWithLifecycle(
                true
            )
            val batteryIgnored by ignoringBatteryOptimizations.collectAsStateWithLifecycle(false)

            FrpTheme(themeMode = currentTheme) {
                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.onboarding_title)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back_24dp),
                                    contentDescription = stringResource(R.string.content_desc_back)
                                )
                            }
                        })
                }) { contentPadding ->
                    OnboardingContent(
                        contentPadding = contentPadding,
                        notificationGranted = notificationGranted,
                        batteryOptimizationIgnored = batteryIgnored,
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onRequestBatteryOptimization = { requestIgnoreBatteryOptimization() },
                        onContinue = { finishOnboarding() })
                }
            }
        }
    }

    private fun updateNotificationPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            notificationPermissionGranted.value = granted
        } else {
            // Android 13 以下无需动态申请通知权限
            notificationPermissionGranted.value = true
        }
    }

    private fun updateBatteryOptimizationStatus() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        ignoringBatteryOptimizations.value =
            powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        // 用户同意后系统会将应用加入白名单，拒绝则保持原状态
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            batteryOptimizationLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // 某些设备可能缺少该入口，记录日志便于排查
            Log.w("Onboarding", "Battery optimization request activity not found: ${e.message}")
        }
    }

    private fun finishOnboarding() {
        preferences.edit {
            putBoolean(PreferencesKey.FIRST_LAUNCH_DONE, true)
        }
        // 返回主界面，避免叠加过多的 Activity
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        finish()
    }

    @Composable
    private fun StatusText(active: Boolean, inactiveText: String) {
        val status = if (active) stringResource(R.string.onboarding_status_done) else inactiveText
        val color =
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Text(text = status, color = color, style = MaterialTheme.typography.bodyMedium)
    }

    @Composable
    private fun OnboardingCard(
        title: String,
        description: String,
        status: @Composable () -> Unit,
        actionLabel: String,
        onAction: () -> Unit,
        enabled: Boolean = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                status()
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAction, enabled = enabled) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }

    @Composable
    fun OnboardingContent(
        contentPadding: PaddingValues,
        notificationGranted: Boolean,
        batteryOptimizationIgnored: Boolean,
        onRequestNotificationPermission: () -> Unit,
        onRequestBatteryOptimization: () -> Unit,
        onContinue: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        val showNotificationAction = remember(notificationGranted) { !notificationGranted }
        val showBatteryAction = remember(batteryOptimizationIgnored) { !batteryOptimizationIgnored }

        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            OnboardingCard(
                title = stringResource(R.string.onboarding_notification_title),
                description = stringResource(R.string.onboarding_notification_desc),
                status = {
                    StatusText(
                        active = notificationGranted,
                        inactiveText = stringResource(R.string.onboarding_notification_status_missing)
                    )
                },
                actionLabel = stringResource(R.string.onboarding_notification_action),
                onAction = onRequestNotificationPermission,
                enabled = showNotificationAction
            )

            OnboardingCard(
                title = stringResource(R.string.onboarding_battery_title),
                description = stringResource(R.string.onboarding_battery_desc),
                status = {
                    StatusText(
                        active = batteryOptimizationIgnored,
                        inactiveText = stringResource(R.string.onboarding_battery_status_missing)
                    )
                },
                actionLabel = stringResource(R.string.onboarding_battery_action),
                onAction = onRequestBatteryOptimization,
                enabled = showBatteryAction
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onContinue()
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun OnboardingPreview() {
        FrpTheme(themeMode = ThemeModeKeys.LIGHT) {
            OnboardingContent(
                contentPadding = PaddingValues(0.dp),
                notificationGranted = false,
                batteryOptimizationIgnored = false,
                onRequestNotificationPermission = {},
                onRequestBatteryOptimization = {},
                onContinue = {})
        }
    }
}
