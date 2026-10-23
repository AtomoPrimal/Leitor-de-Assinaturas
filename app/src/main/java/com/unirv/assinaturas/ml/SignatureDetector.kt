package com.unirv.assinaturas.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.unirv.assinaturas.model.BoundingBox
import com.unirv.assinaturas.model.ImageDimension
import com.unirv.assinaturas.model.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class SignatureDetector(private val context: Context) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    var currentModelName: String = "best.onnx"
        private set

    init {
        loadEmbeddedModel("best.onnx")
    }

    /**
     * RF1: Carrega o modelo ONNX embarcado em assets
     */
    fun loadEmbeddedModel(assetName: String = "best.onnx"): Boolean {
        return try {
            val modelBytes = context.assets.open(assetName).use { it.readBytes() }
            ortSession?.close()
            ortSession = ortEnv.createSession(modelBytes, OrtSession.SessionOptions())
            currentModelName = assetName
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * RF1: Carrega modelo ONNX a partir do armazenamento do dispositivo
     */
    fun loadExternalModel(file: File): Boolean {
        return try {
            val modelBytes = FileInputStream(file).use { it.readBytes() }
            ortSession?.close()
            ortSession = ortEnv.createSession(modelBytes, OrtSession.SessionOptions())
            currentModelName = file.name
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * RF4 & RF5: Pipeline de inferência 100% on-device e cálculo geométrico
     */
    suspend fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.45f,
        iouThreshold: Float = 0.45f
    ): InferenceResult = withContext(Dispatchers.Default) {
        val session = ortSession ?: throw IllegalStateException("Modelo ONNX não inicializado")
        val startTime = System.currentTimeMillis()

        val origWidth = bitmap.width
        val origHeight = bitmap.height
        val targetSize = 640

        // 1. Pré-processamento com Letterbox (preserva a proporção e aspecto da assinatura sem distorcer)
        val scale = minOf(targetSize.toFloat() / origWidth, targetSize.toFloat() / origHeight)
        val scaledW = (origWidth * scale).toInt()
        val scaledH = (origHeight * scale).toInt()
        val padX = (targetSize - scaledW) / 2f
        val padY = (targetSize - scaledH) / 2f

        val letterboxed = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(letterboxed)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114)) // padding padrão YOLO (cinza 114)

        val scaledBmp = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        canvas.drawBitmap(scaledBmp, padX, padY, null)

        val floatBuffer = ByteBuffer.allocateDirect(1 * 3 * targetSize * targetSize * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val pixels = IntArray(targetSize * targetSize)
        letterboxed.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // Canais R, G e B separados (Planar NCHW) normalizados em [0.0, 1.0]
        val totalPixels = targetSize * targetSize
        val rOffset = 0
        val gOffset = totalPixels
        val bOffset = totalPixels * 2

        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255.0f
            val g = ((p shr 8) and 0xFF) / 255.0f
            val b = (p and 0xFF) / 255.0f

            floatBuffer.put(rOffset + i, r)
            floatBuffer.put(gOffset + i, g)
            floatBuffer.put(bOffset + i, b)
        }
        floatBuffer.rewind()

        // 2. Criação do Tensor de Entrada e Execução ONNX
        val inputName = session.inputNames.iterator().next()
        val inputShape = longArrayOf(1, 3, targetSize.toLong(), targetSize.toLong())
        val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, inputShape)

        val output = session.run(mapOf(inputName to inputTensor))
        val outputTensor = output[0] as OnnxTensor

        // Formato YOLOv8: [1, 5, 8400]
        @Suppress("UNCHECKED_CAST")
        val rawData = outputTensor.value as Array<Array<FloatArray>>
        val matrix = rawData[0] // 5 arrays de 8400 posições

        val cxArray = matrix[0]
        val cyArray = matrix[1]
        val wArray = matrix[2]
        val hArray = matrix[3]
        val confArray = matrix[4]
        val numCandidates = cxArray.size

        // 3. Filtragem e Des-letterbox (reverte as coordenadas para o tamanho real da foto)
        val candidateBoxes = mutableListOf<RawBox>()

        for (i in 0 until numCandidates) {
            val conf = confArray[i]
            if (conf >= confidenceThreshold) {
                val cxIn640 = cxArray[i]
                val cyIn640 = cyArray[i]
                val wIn640 = wArray[i]
                val hIn640 = hArray[i]

                // Remove o padding do letterbox e reescala para as dimensões reais da foto
                val cx = (cxIn640 - padX) / scale
                val cy = (cyIn640 - padY) / scale
                val w = wIn640 / scale
                val h = hIn640 / scale

                val xMin = max(0f, cx - (w / 2f))
                val yMin = max(0f, cy - (h / 2f))
                val xMax = min(origWidth.toFloat(), cx + (w / 2f))
                val yMax = min(origHeight.toFloat(), cy + (h / 2f))

                candidateBoxes.add(
                    RawBox(
                        conf = conf,
                        xMin = xMin,
                        yMin = yMin,
                        xMax = xMax,
                        yMax = yMax,
                        w = xMax - xMin,
                        h = yMax - yMin
                    )
                )
            }
        }

        // 4. Non-Maximum Suppression (NMS)
        val filteredBoxes = applyNMS(candidateBoxes, iouThreshold)

        // 5. Cálculo das Métricas Geométricas Obrigatórias (RF5)
        var idCounter = 1
        var sumConf = 0f
        val finalBoxes = filteredBoxes.map { raw ->
            sumConf += raw.conf
            val wPx = raw.w
            val hPx = raw.h
            // Fórmula do Enunciado: Xcentroide = Xmin + (Wpx / 2), Ycentroide = Ymin + (Hpx / 2)
            val cX = raw.xMin + (wPx / 2f)
            val cY = raw.yMin + (hPx / 2f)
            val area = wPx * hPx

            BoundingBox(
                boxId = idCounter++,
                classLabel = "signature",
                confidence = raw.conf,
                widthPx = wPx,
                heightPx = hPx,
                centroidX = cX,
                centroidY = cY,
                areaPx2 = area,
                xMin = raw.xMin,
                yMin = raw.yMin,
                xMax = raw.xMax,
                yMax = raw.yMax
            )
        }

        val execTime = System.currentTimeMillis() - startTime
        val avgConf = if (finalBoxes.isNotEmpty()) sumConf / finalBoxes.size else 0f
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val sessionId = "sessao_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

        inputTensor.close()
        output.close()

        InferenceResult(
            sessionId = sessionId,
            timestamp = sdf.format(Date()),
            modelName = currentModelName,
            executionTimeMs = execTime,
            totalObjects = finalBoxes.size,
            averageConfidence = avgConf,
            imageDimension = ImageDimension(origWidth, origHeight),
            boundingBoxes = finalBoxes
        )
    }

    private fun applyNMS(boxes: List<RawBox>, iouThreshold: Float): List<RawBox> {
        val sorted = boxes.sortedByDescending { it.conf }.toMutableList()
        val selected = mutableListOf<RawBox>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (computeIoU(best, candidate) >= iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selected
    }

    private fun computeIoU(a: RawBox, b: RawBox): Float {
        val interXmin = max(a.xMin, b.xMin)
        val interYmin = max(a.yMin, b.yMin)
        val interXmax = min(a.xMax, b.xMax)
        val interYmax = min(a.yMax, b.yMax)

        val interW = max(0f, interXmax - interXmin)
        val interH = max(0f, interYmax - interYmin)
        val interArea = interW * interH

        val areaA = a.w * a.h
        val areaB = b.w * b.h
        val unionArea = areaA + areaB - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    fun close() {
        ortSession?.close()
        ortEnv.close()
    }

    private data class RawBox(
        val conf: Float,
        val xMin: Float,
        val yMin: Float,
        val xMax: Float,
        val yMax: Float,
        val w: Float,
        val h: Float
    )
}
