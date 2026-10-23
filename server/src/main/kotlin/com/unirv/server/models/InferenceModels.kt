package com.unirv.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageDimension(
    @SerialName("largura_px")
    val widthPx: Int,
    @SerialName("altura_px")
    val heightPx: Int
)

@Serializable
data class BoundingBox(
    @SerialName("id_caixa")
    val boxId: Int,
    @SerialName("rotulo_classe")
    val classLabel: String = "signature",
    @SerialName("confianca")
    val confidence: Float,
    @SerialName("largura_px")
    val widthPx: Float,
    @SerialName("altura_px")
    val heightPx: Float,
    @SerialName("centroide_x")
    val centroidX: Float,
    @SerialName("centroide_y")
    val centroidY: Float,
    @SerialName("area_px2")
    val areaPx2: Float
)

@Serializable
data class InferencePayload(
    @SerialName("id_sessao")
    val sessionId: String,
    @SerialName("data_hora")
    val timestamp: String,
    @SerialName("nome_modelo")
    val modelName: String,
    @SerialName("tempo_execucao_ms")
    val executionTimeMs: Long,
    @SerialName("total_objetos")
    val totalObjects: Int,
    @SerialName("confianca_media")
    val averageConfidence: Float,
    @SerialName("dimensao_imagem")
    val imageDimension: ImageDimension,
    @SerialName("caixas_delimitadoras")
    val boundingBoxes: List<BoundingBox>
)
