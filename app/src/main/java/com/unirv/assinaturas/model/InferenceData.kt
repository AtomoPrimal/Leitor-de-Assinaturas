package com.unirv.assinaturas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    var widthPx: Float,
    @SerialName("altura_px")
    var heightPx: Float,
    @SerialName("centroide_x")
    var centroidX: Float,
    @SerialName("centroide_y")
    var centroidY: Float,
    @SerialName("area_px2")
    var areaPx2: Float,
    // Coordenadas absolutas na imagem original para desenho
    val xMin: Float = 0f,
    val yMin: Float = 0f,
    val xMax: Float = 0f,
    val yMax: Float = 0f
) {
    fun getAreaPx(): Float = widthPx * heightPx
}

@Serializable
data class InferenceResult(
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
    val boundingBoxes: List<BoundingBox>,
    var syncedToServer: Boolean = false,
    var imagePath: String? = null
) {
    val isSigned: Boolean get() = totalObjects > 0

    fun computeCentroids() {
        for (box in boundingBoxes) {
            box.centroidX = box.xMin + (box.widthPx / 2f)
            box.centroidY = box.yMin + (box.heightPx / 2f)
            box.areaPx2 = box.getAreaPx()
        }
    }

    fun toJsonPayload(): String {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        return json.encodeToString(this)
    }
}
