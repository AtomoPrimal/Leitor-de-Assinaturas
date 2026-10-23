package com.unirv.assinaturas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unirv.assinaturas.theme.*

@Composable
fun HeroStatusBanner(
    isSigned: Boolean,
    signatureCount: Int,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSigned) EmeraldSuccessLight else AmberWarningLight
    val borderColor = if (isSigned) EmeraldSuccess.copy(alpha = 0.3f) else AmberWarning.copy(alpha = 0.3f)
    val titleColor = if (isSigned) EmeraldSuccessText else AmberWarningText
    val iconBgColor = if (isSigned) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
    val iconColor = if (isSigned) EmeraldSuccess else AmberWarning

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSigned) "✓" else "⚠",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            Column {
                Text(
                    text = if (isSigned) "DOCUMENTO ASSINADO" else "DOCUMENTO NÃO ASSINADO",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = if (isSigned) "$signatureCount assinatura(s) identificada(s)"
                    else "Nenhuma assinatura identificada no documento",
                    fontSize = 12.sp,
                    color = titleColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
