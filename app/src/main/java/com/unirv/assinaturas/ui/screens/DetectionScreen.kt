package com.unirv.assinaturas.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unirv.assinaturas.model.InferenceResult
import com.unirv.assinaturas.theme.*
import com.unirv.assinaturas.ui.components.AnnotatedImageView
import com.unirv.assinaturas.ui.components.HeroStatusBanner
import com.unirv.assinaturas.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun DetectionScreen(
    viewModel: MainViewModel,
    onNavigateToAudit: (InferenceResult) -> Unit
) {
    val context = LocalContext.current
    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val currentInference by viewModel.currentInference.collectAsState()
    val confThreshold by viewModel.confidenceThreshold.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Controla se exibe Tela 1 (Configuração & Entrada) ou Tela 2 (Resultados)
    var showResults by remember { mutableStateOf(false) }

    // Launcher para Tirar Foto (Câmera Nativa)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setSelectedBitmap(bitmap)
            showResults = false
        }
    }

    // Launcher para Galeria de Imagens
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            viewModel.setSelectedBitmap(bitmap)
            showResults = false
        }
    }

    // Launcher para selecionar arquivo .onnx externo (RF1)
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val tempFile = File(context.cacheDir, "custom_model.onnx")
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                val success = viewModel.loadExternalModel(tempFile)
                if (success) {
                    Toast.makeText(context, "Modelo ONNX carregado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Falha ao carregar modelo ONNX", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!showResults || currentInference == null) {
            // ================= TELA 1: CONFIGURAÇÃO & ENTRADA (Fig 3a) =================

            // Card Gerenciador de Modelo (RF1)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modelo YOLO (ONNX)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateTextSecondary
                        )
                        Text(
                            text = "✓ Ativo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = EmeraldSuccess
                        )
                    }

                    Text(
                        text = viewModel.currentModelName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SlateTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { modelPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Selecionar .onnx", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.loadEmbeddedModel()
                                Toast.makeText(context, "Modelo best.onnx padrão restaurado", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Restaurar", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Card de Aquisição de Imagem (RF2)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Capturar ou Selecionar Imagem",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tirar Foto")
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Galeria")
                        }
                    }

                    // Ajuste Dinâmico do Limiar de Confiança (RF3)
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Limiar de Confiança",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(confThreshold * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                        Slider(
                            value = confThreshold,
                            onValueChange = { viewModel.setConfidenceThreshold(it) },
                            valueRange = 0.15f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = BluePrimary,
                                activeTrackColor = BluePrimary
                            )
                        )
                    }
                }
            }

            // Pré-visualização da Imagem
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pré-visualização do Documento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )

                    AnnotatedImageView(
                        bitmap = currentBitmap,
                        boxes = emptyList(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Botão Executar Inferência
            Button(
                onClick = {
                    currentBitmap?.let { bmp ->
                        viewModel.processImage(bmp) {
                            showResults = true
                        }
                    } ?: Toast.makeText(context, "Selecione ou tire uma foto primeiro!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(10.dp),
                enabled = !isProcessing && currentBitmap != null
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Executar Inferência", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            // ================= TELA 2: RESULTADOS & VISUALIZAÇÃO ANOTADA (Fig 3b) =================

            val inference = currentInference!!

            // Card Hero: DOCUMENTO ASSINADO / NÃO ASSINADO
            HeroStatusBanner(
                isSigned = inference.isSigned,
                signatureCount = inference.totalObjects
            )

            // Grid de Métricas (RF4: contagem, tempo em ms e confiança)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Assinaturas",
                    value = inference.totalObjects.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Confiança",
                    value = if (inference.isSigned) "${(inference.averageConfidence * 100).toInt()}%" else "--",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Tempo (NPU/CPU)",
                    value = "${inference.executionTimeMs}ms",
                    modifier = Modifier.weight(1f)
                )
            }

            // Imagem Anotada com as Bounding Boxes (RF5)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Imagem Anotada (Caixas e Centróides)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )

                    AnnotatedImageView(
                        bitmap = currentBitmap,
                        boxes = inference.boundingBoxes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Botões de Ação da Tela 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showResults = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nova Análise")
                }

                Button(
                    onClick = { onNavigateToAudit(inference) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ver Auditoria")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BluePrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
        }
    }
}
