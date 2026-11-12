package br.androidports.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.androidports.BuildConfig
import br.androidports.data.local.SettingsRepository
import br.androidports.data.local.ThemeMode
import br.androidports.di.AppContainer
import br.androidports.ui.components.PortsButton
import br.androidports.ui.components.ScreenHeader
import br.androidports.ui.components.SectionLabel
import br.androidports.ui.components.SettingRow
import br.androidports.ui.components.ToggleRow
import br.androidports.ui.theme.PortsTheme
import br.androidports.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = PortsTheme.colors
    val context = LocalContext.current

    var serverField by rememberSaveable(settings.serverUrl) {
        mutableStateOf(settings.serverUrl)
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(4_000)
            viewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 128.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Ajustes",
                subtitle = "Aparência, resposta tátil e conexão com o servidor.",
            )
        }

        item {
            SectionLabel("Aparência")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                ThemePicker(
                    selected = settings.themeMode,
                    onSelect = viewModel::setTheme,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        item {
            SectionLabel("Movimento e resposta")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                ToggleRow(
                    title = "Resposta tátil",
                    subtitle = "Vibração curta ao curtir, salvar e navegar.",
                    checked = settings.haptics,
                    onCheckedChange = viewModel::setHaptics,
                )
                HorizontalDivider(color = colors.hairline)
                ToggleRow(
                    title = "Reduzir animações",
                    subtitle = "Transições instantâneas e shimmer estático.",
                    checked = settings.reduceMotion,
                    onCheckedChange = viewModel::setReduceMotion,
                )
            }
        }

        item {
            SectionLabel("Servidor")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Text(
                    text = "URL do servidor local",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = serverField,
                    onValueChange = { serverField = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    placeholder = { Text("http://10.0.2.2:3000") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.hairline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                        focusedPlaceholderColor = colors.textTertiary,
                        unfocusedPlaceholderColor = colors.textTertiary,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PortsButton(
                        label = "Salvar",
                        onClick = { viewModel.saveServerUrl(serverField) },
                        modifier = Modifier.weight(1f),
                    )
                    PortsButton(
                        label = if (connection is ConnectionState.Testing) "Testando…" else "Testar conexão",
                        onClick = { viewModel.testConnection(serverField) },
                        filled = false,
                        icon = Icons.Outlined.Refresh,
                        enabled = connection !is ConnectionState.Testing,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                ConnectionStatus(connection)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Emulador: 10.0.2.2 · USB: 127.0.0.1 · Termux: 127.0.0.1",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }

        item {
            SectionLabel("Dados")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                SettingRow(
                    title = "Limpar cache",
                    subtitle = "Remove notícias em cache e respostas HTTP. As salvas permanecem.",
                    onClick = viewModel::clearCache,
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                        )
                    },
                )
                if (uiState.message != null) {
                    Row(
                        modifier = Modifier.padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = uiState.message.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("Sobre")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                SettingRow(
                    title = "Versão",
                    trailing = {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textTertiary,
                        )
                    },
                )
                HorizontalDivider(color = colors.hairline)
                SettingRow(
                    title = "Repositório no GitHub",
                    subtitle = "Código, servidor e builds automatizados.",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/deivid22srk/android-ports-br"),
                                ),
                            )
                        }
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                )
                HorizontalDivider(color = colors.hairline)
                SettingRow(
                    title = "Tipografia",
                    subtitle = "Doto e Inter · licença SIL Open Font License.",
                )
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ANDROID PORTS BR · FEITO PARA A COMUNIDADE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PortsTheme.colors.surface)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ThemePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val colors = PortsTheme.colors
    val options = listOf(
        ThemeMode.DARK to "Escuro",
        ThemeMode.LIGHT to "Claro",
        ThemeMode.SYSTEM to "Sistema",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (mode, label) ->
            val active = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.textPrimary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) colors.background else colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    val colors = PortsTheme.colors
    when (state) {
        ConnectionState.Idle -> Unit

        ConnectionState.Testing -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Testando conexão…",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }

        is ConnectionState.Success -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Conectado · ${state.newsCount} notícias · modo ${state.mode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }

        is ConnectionState.Failure -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
