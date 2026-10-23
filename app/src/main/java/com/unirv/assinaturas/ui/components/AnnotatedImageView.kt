package com.unirv.assinaturas.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.unirv.assinaturas.model.BoundingBox
import com.unirv.assinaturas.theme.BluePrimary
import com.unirv.assinaturas.theme.CoralDanger
import com.unirv.assinaturas.theme.SlateTextPrimary

@Composable
fun AnnotatedImageView(
    bitmap: Bitmap?,
    boxes: List<BoundingBox>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Imagem do documento",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Sobreposição de caixas delimitadoras e centróides (RF5)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()

                // Calcula escala mantendo proporção (ContentScale.Fit)
                val scale = minOf(canvasW / imgW, canvasH / imgH)
                val offsetX = (canvasW - (imgW * scale)) / 2f
                val offsetY = (canvasH - (imgH * scale)) / 2f

                for (box in boxes) {
                    val bx = (box.xMin * scale) + offsetX
                    val by = (box.yMin * scale) + offsetY
                    val bw = box.widthPx * scale
                    val bh = box.heightPx * scale

                    // Caixa delimitadora
                    drawRect(
                        color = BluePrimary,
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh),
                        style = Stroke(width = 4f)
                    )

                    // Preenchimento semitransparente
                    drawRect(
                        color = BluePrimary.copy(alpha = 0.15f),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh)
                    )

                    // Centróide (RF5)
                    val cx = (box.centroidX * scale) + offsetX
                    val cy = (box.centroidY * scale) + offsetY

                    drawCircle(
                        color = CoralDanger,
                        radius = 6f,
                        center = Offset(cx, cy)
                    )

                    // Mira do centróide
                    drawLine(
                        color = CoralDanger,
                        start = Offset(cx - 12f, cy),
                        end = Offset(cx + 12f, cy),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = CoralDanger,
                        start = Offset(cx, cy - 12f),
                        end = Offset(cx, cy + 12f),
                        strokeWidth = 2f
                    )
                }
            }
        } else {
            Text(
                text = "Nenhuma imagem selecionada",
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
