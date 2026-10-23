package com.unirv.assinaturas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unirv.assinaturas.model.InferenceResult
import com.unirv.assinaturas.theme.BluePrimary
import com.unirv.assinaturas.theme.LeitorAssinaturasTheme
import com.unirv.assinaturas.theme.SlateBackground
import com.unirv.assinaturas.theme.SlateSurface
import com.unirv.assinaturas.theme.SlateTextPrimary
import com.unirv.assinaturas.ui.screens.DetectionScreen
import com.unirv.assinaturas.ui.screens.HistoryScreen
import com.unirv.assinaturas.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LeitorAssinaturasTheme {
                var currentTab by remember { mutableStateOf(NavTab.DETECTAR) }
                var selectedAuditResult by remember { mutableStateOf<InferenceResult?>(null) }
                var showIpDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Leitor de Assinaturas",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            },
                            actions = {
                                IconButton(onClick = { showIpDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Configurar IP do Servidor",
                                        tint = BluePrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = SlateSurface
                            )
                        )
                    },
                    bottomBar = {
                        // Bottom Navigation Bar Obrigatória (RF8 / Seção 7)
                        NavigationBar(
                            containerColor = SlateSurface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == NavTab.DETECTAR,
                                onClick = {
                                    currentTab = NavTab.DETECTAR
                                    selectedAuditResult = null
                                },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                label = { Text("Detectar") }
                            )
                            NavigationBarItem(
                                selected = currentTab == NavTab.HISTORICO,
                                onClick = {
                                    currentTab = NavTab.HISTORICO
                                    selectedAuditResult = null
                                },
                                icon = { Icon(Icons.Default.List, contentDescription = null) },
                                label = { Text("Histórico") }
                            )
                        }
                    },
                    containerColor = SlateBackground
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            NavTab.DETECTAR -> {
                                DetectionScreen(
                                    viewModel = viewModel,
                                    onNavigateToAudit = { result ->
                                        selectedAuditResult = result
                                        currentTab = NavTab.HISTORICO
                                    }
                                )
                            }
                            NavTab.HISTORICO -> {
                                HistoryScreen(
                                    viewModel = viewModel,
                                    selectedAuditResult = selectedAuditResult,
                                    onClearAuditSelection = { selectedAuditResult = null },
                                    onSelectAudit = { result -> selectedAuditResult = result }
                                )
                            }
                        }
                    }
                }

                if (showIpDialog) {
                    val serverUrl by viewModel.serverUrl.collectAsState()
                    var inputUrl by remember { mutableStateOf(serverUrl) }

                    AlertDialog(
                        onDismissRequest = { showIpDialog = false },
                        title = { Text("Configurar Servidor Backend") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Insira o endereço IP da máquina executando o servidor Ktor:",
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    label = { Text("URL Base") },
                                    singleLine = true
                                )
                                Text(
                                    text = "Ex: http://192.168.1.15:8081 ou http://10.0.2.2:8081",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.updateServerUrl(inputUrl)
                                showIpDialog = false
                            }) {
                                Text("Salvar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showIpDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class NavTab {
    DETECTAR,
    HISTORICO
}
