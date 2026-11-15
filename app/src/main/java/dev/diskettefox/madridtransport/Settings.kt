package dev.diskettefox.madridtransport

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diskettefox.madridtransport.ui.theme.MadridTransportTheme

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MadridTransportTheme {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val currentThemeMode = ThemeManager.themeMode
    val isOledMode = ThemeManager.isOledMode

    val themeSummaryText = when (currentThemeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val favoritesList = FavoritesManager.getFavorites(context)
                if (favoritesList.isEmpty()) {
                    Toast.makeText(context, R.string.settings_export_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val jsonString = FavoritesManager.exportFavoritesToJson(context)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, R.string.settings_export_success, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, R.string.settings_export_empty, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonContent != null && FavoritesManager.importFavoritesFromJson(context, jsonContent)) {
                    Toast.makeText(context, R.string.settings_import_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.settings_import_error, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, R.string.settings_import_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeOptionRow(
                        title = stringResource(R.string.theme_system),
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = {
                            ThemeManager.setThemeMode(context, ThemeMode.SYSTEM)
                            showThemeDialog = false
                            (context as? android.app.Activity)?.recreate()
                        }
                    )
                    ThemeOptionRow(
                        title = stringResource(R.string.theme_light),
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = {
                            ThemeManager.setThemeMode(context, ThemeMode.LIGHT)
                            showThemeDialog = false
                            (context as? android.app.Activity)?.recreate()
                        }
                    )
                    ThemeOptionRow(
                        title = stringResource(R.string.theme_dark),
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = {
                            ThemeManager.setThemeMode(context, ThemeMode.DARK)
                            showThemeDialog = false
                            (context as? android.app.Activity)?.recreate()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_favorites)) },
            text = { Text(stringResource(R.string.settings_clear_favorites_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        FavoritesManager.clearFavorites(context)
                        Toast.makeText(context, R.string.settings_favorites_cleared, Toast.LENGTH_SHORT).show()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_left),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryHeader(stringResource(R.string.settings_category_general))

            CardSection {
                SettingRow(
                    label = stringResource(R.string.settings_theme),
                    summary = themeSummaryText,
                    iconPainter = painterResource(id = R.drawable.outline_brush_24),
                    onClick = { showThemeDialog = true }
                )
                InfoDivider()
                SettingToggleRow(
                    label = stringResource(R.string.settings_oled),
                    summary = stringResource(R.string.settings_oled_summary),
                    iconPainter = painterResource(id = R.drawable.eye),
                    checked = isOledMode,
                    onCheckedChange = { 
                        ThemeManager.setOledMode(context, it)
                        (context as? android.app.Activity)?.recreate()
                    }
                )
            }

            CategoryHeader(stringResource(R.string.settings_category_data))

            CardSection {
                SettingRow(
                    label = stringResource(R.string.settings_export_favorites),
                    summary = stringResource(R.string.settings_export_favorites_summary),
                    iconPainter = painterResource(id = R.drawable.ic_file_download_24dp),
                    onClick = { exportLauncher.launch("madrid_transport_favorites.json") }
                )
                InfoDivider()
                SettingRow(
                    label = stringResource(R.string.settings_import_favorites),
                    summary = stringResource(R.string.settings_import_favorites_summary),
                    iconPainter = painterResource(id = R.drawable.ic_file_upload_24dp),
                    onClick = { importLauncher.launch("*/*") }
                )
                InfoDivider()
                SettingRow(
                    label = stringResource(R.string.settings_clear_favorites),
                    summary = stringResource(R.string.settings_clear_favorites_summary),
                    iconPainter = painterResource(id = R.drawable.ic_favorite_24dp),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true }
                )
            }

            CategoryHeader(stringResource(R.string.settings_category_account))

            CardSection {
                SettingRow(
                    label = stringResource(R.string.settings_account_status),
                    summary = stringResource(R.string.settings_account_summary),
                    iconPainter = painterResource(id = R.drawable.ic_emt),
                    onClick = {
                        context.startActivity(Intent(context, Login::class.java))
                    }
                )
                InfoDivider()
                SettingRow(
                    label = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    iconPainter = painterResource(id = R.drawable.outline_info_24),
                    onClick = {
                        context.startActivity(Intent(context, About::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            letterSpacing = 1.2.sp
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun CardSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    summary: String,
    iconPainter: Painter,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    summary: String,
    iconPainter: Painter,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.25.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    MadridTransportTheme {
        SettingsScreen(onBackClick = {})
    }
}
