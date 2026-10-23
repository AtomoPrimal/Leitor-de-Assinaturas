package com.unirv.assinaturas.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unirv.assinaturas.ml.SignatureDetector
import com.unirv.assinaturas.model.InferenceResult
import com.unirv.assinaturas.network.InferenceApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = SignatureDetector(application)
    private val apiClient = InferenceApiClient()

    private val _confidenceThreshold = MutableStateFlow(0.45f)
    val confidenceThreshold: StateFlow<Float> = _confidenceThreshold.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _currentInference = MutableStateFlow<InferenceResult?>(null)
    val currentInference: StateFlow<InferenceResult?> = _currentInference.asStateFlow()

    private val _history = MutableStateFlow<List<InferenceResult>>(emptyList())
    val history: StateFlow<List<InferenceResult>> = _history.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _serverUrl = MutableStateFlow(apiClient.getBaseUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    val currentModelName: String get() = detector.currentModelName

    fun setConfidenceThreshold(value: Float) {
        _confidenceThreshold.value = value
    }

    fun setSelectedBitmap(bitmap: Bitmap?) {
        _currentBitmap.value = bitmap
    }

    fun updateServerUrl(url: String) {
        apiClient.updateBaseUrl(url)
        _serverUrl.value = apiClient.getBaseUrl()
    }

    fun loadExternalModel(file: File): Boolean {
        return detector.loadExternalModel(file)
    }

    fun loadEmbeddedModel(): Boolean {
        return detector.loadEmbeddedModel("best.onnx")
    }

    /**
     * Executa inferência On-Device 100% offline e tenta sincronizar telemetria com a API
     */
    fun processImage(bitmap: Bitmap, onComplete: (InferenceResult) -> Unit = {}) {
        viewModelScope.launch {
            _isProcessing.value = true
            _currentBitmap.value = bitmap
            try {
                val result = detector.detect(
                    bitmap = bitmap,
                    confidenceThreshold = _confidenceThreshold.value
                )

                // Salva a foto do documento no armazenamento interno (base de dados/auditoria)
                try {
                    val historyDir = File(getApplication<Application>().filesDir, "history").apply { mkdirs() }
                    val imageFile = File(historyDir, "${result.sessionId}.jpg")
                    java.io.FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    result.imagePath = imageFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _currentInference.value = result
                _history.value = listOf(result) + _history.value

                // Envia em segundo plano para o servidor (RF6)
                sendTelemetry(result)

                onComplete(result)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadBitmapFromPath(path: String?): Bitmap? {
        if (path == null) return null
        val file = File(path)
        return if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun sendTelemetry(result: InferenceResult) {
        viewModelScope.launch {
            try {
                val response = apiClient.apiService.sendInferenceData(result)
                if (response.isSuccessful) {
                    result.syncedToServer = true
                    // Atualiza flag no histórico local
                    _history.value = _history.value.map {
                        if (it.sessionId == result.sessionId) it.copy(syncedToServer = true) else it
                    }
                }
            } catch (e: Exception) {
                // Servidor offline: continua normalmente sem travar o app móvel
                result.syncedToServer = false
            }
        }
    }

    fun fetchServerHistory() {
        viewModelScope.launch {
            try {
                val remoteList = apiClient.apiService.getInferenceHistory()
                if (remoteList.isNotEmpty()) {
                    val merged = (remoteList + _history.value).distinctBy { it.sessionId }
                    _history.value = merged
                }
            } catch (e: Exception) {
                // Servidor não alcançável
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
