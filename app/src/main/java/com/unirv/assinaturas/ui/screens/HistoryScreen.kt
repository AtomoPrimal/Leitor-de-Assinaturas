package com.unirv.assinaturas.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unirv.assinaturas.model.BoundingBox
import com.unirv.assinaturas.model.InferenceResult
import com.unirv.assinaturas.theme.*
import com.unirv.assinaturas.ui.components.AnnotatedImageView
import com.unirv.assinaturas.viewmodel.MainViewModel

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    selectedAuditResult: InferenceResult?,
    onClearAuditSelection: () -> Unit,
    onSelectAudit: (InferenceResult) -> Unit
) {
    val historyList by viewModel.history.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchServerHistory()
    }

    if (selectedAuditResult != null) {
        // ================= TELA 4: DETALHAMENTO GEOMÉTRICO (Fig 4b / RF8) =================
        AuditDetailScreen(
            result = selectedAuditResult,
            viewModel = viewModel,
            onBack = onClearAuditSelection
        )
    } else {
        // ================= TELA 3: HISTÓRICO DE INFERÊNCIAS (Fig 4a / RF8) =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header do Histórico
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Histórico de Análises",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "Total: ${historyList.size} sessões registradas",
                            fontSize = 12.sp,
                            color = SlateTextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.fetchServerHistory() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sincronizar", fontSize = 11.sp)
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma análise realizada ainda.\nFaça a primeira leitura na aba Detectar!",
                        color = SlateTextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList) { item ->
                        HistoryItemCard(item = item, viewModel = viewModel, onClick = { onSelectAudit(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: InferenceResult, viewModel: MainViewModel, onClick: () -> Unit) {
    val thumbnail = remember(item.imagePath) { viewModel.loadBitmapFromPath(item.imagePath) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Foto do documento",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.sessionId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SlateTextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (item.isSigned) EmeraldSuccessLight else AmberWarningLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isSigned) "Assinado (${item.totalObjects})" else "Não Assinado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isSigned) EmeraldSuccessText else AmberWarningText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${item.timestamp.take(16)}",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                    Text(
                        text = "⚡ ${item.executionTimeMs}ms",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                    Text(
                        text = if (item.syncedToServer) "✓ Sincronizado" else "☁ Local",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.syncedToServer) EmeraldSuccess else SlateTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditDetailScreen(
    result: InferenceResult,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val savedBitmap = remember(result.imagePath) { viewModel.loadBitmapFromPath(result.imagePath) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barra Superior de Voltar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Auditoria Geométrica",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SlateTextPrimary
                )
                Text(
                    text = result.sessionId,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlateTextSecondary
                )
            }
        }

        // Exibe a Foto Original Salva com as Caixas Sobrepostas (Evidência Visual)
        if (savedBitmap != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Evidência Fotográfica do Documento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                    AnnotatedImageView(
                        bitmap = savedBitmap,
                        boxes = result.boundingBoxes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Resumo
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Resumo da Inferência",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total de Caixas:", fontSize = 12.sp, color = SlateTextSecondary)
                    Text("${result.totalObjects}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tempo de Execução:", fontSize = 12.sp, color = SlateTextSecondary)
                    Text("${result.executionTimeMs} ms", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Resolução da Foto:", fontSize = 12.sp, color = SlateTextSecondary)
                    Text("${result.imageDimension.widthPx}x${result.imageDimension.heightPx} px", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text(
            text = "Métricas Geométricas por Caixa (RF5)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = SlateTextSecondary
        )

        if (result.boundingBoxes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma assinatura foi delimitada nesta sessão.", color = SlateTextMuted, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.boundingBoxes.forEach { box ->
                    BoxAuditCard(box = box)
                }
            }
        }
    }
}

@Composable
private fun BoxAuditCard(box: BoundingBox) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Caixa #${String.format("%02d", box.boxId)} (${box.classLabel})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BluePrimary
                )
                Text(
                    text = "Confiança: ${(box.confidence * 100).toInt()}%",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = EmeraldSuccess
                )
            }

            HorizontalDivider(color = SlateBorder, thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Largura (Wpx): ${box.widthPx.toInt()} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Altura (Hpx): ${box.heightPx.toInt()} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Centróide X: ${String.format("%.1f", box.centroidX)} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Centróide Y: ${String.format("%.1f", box.centroidY)} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "Área (Wpx × Hpx): ${String.format("%.0f", box.areaPx2)} px²",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = SlateTextPrimary
            )
        }
    }
}
