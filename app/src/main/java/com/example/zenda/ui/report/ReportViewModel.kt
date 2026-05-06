package com.example.zenda.ui.report

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zenda.data.model.DangerLevel
import com.example.zenda.data.model.Report
import com.example.zenda.data.model.ReportCategory
import com.example.zenda.data.model.ReportImageSource
import com.example.zenda.data.repository.MockReportRepository
import com.example.zenda.data.repository.ReportRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ReportFormUiState(
    val title: String = "",
    val description: String = "",
    val category: ReportCategory = ReportCategory.OTRO,
    val dangerLevel: DangerLevel = DangerLevel.BAJO,
    val currentLocation: LatLng? = null,
    val imageUri: Uri? = null,
    val cameraCapture: Bitmap? = null,
    val isSubmitting: Boolean = false,
    val submitFinished: Boolean = false,
    val lastError: String? = null
)

class ReportViewModel(
    private val repository: ReportRepository = MockReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportFormUiState())
    val uiState: StateFlow<ReportFormUiState> = _uiState.asStateFlow()

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun setCategory(value: ReportCategory) = _uiState.update { it.copy(category = value) }
    fun setDangerLevel(value: DangerLevel) = _uiState.update { it.copy(dangerLevel = value) }
    fun setCurrentLocation(latLng: LatLng) = _uiState.update { it.copy(currentLocation = latLng) }
    fun setImageUri(uri: Uri?) = _uiState.update {
        it.copy(
            imageUri = uri,
            cameraCapture = if (uri != null) null else it.cameraCapture
        )
    }

    fun setCameraCapture(bitmap: Bitmap?) = _uiState.update {
        it.copy(
            cameraCapture = bitmap,
            imageUri = if (bitmap != null) null else it.imageUri
        )
    }

    fun resetSubmitState() {
        _uiState.update { it.copy(submitFinished = false, lastError = null) }
    }

    fun clearForm() {
        _uiState.value = ReportFormUiState(currentLocation = _uiState.value.currentLocation)
    }

    fun submitReport() {
        val state = _uiState.value
        val loc = state.currentLocation
        if (state.title.isBlank() || loc == null) {
            _uiState.update {
                it.copy(lastError = if (loc == null) "Ubicación no disponible" else "Indica un título")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, lastError = null) }
            val imageSource = when {
                state.imageUri != null -> ReportImageSource.GALLERY
                state.cameraCapture != null -> ReportImageSource.CAMERA
                else -> ReportImageSource.NONE
            }
            val report = Report(
                id = UUID.randomUUID().toString(),
                title = state.title.trim(),
                description = state.description.trim(),
                category = state.category,
                dangerLevel = state.dangerLevel,
                latitude = loc.latitude,
                longitude = loc.longitude,
                imageUri = state.imageUri,
                imageSource = imageSource,
                createdAtEpochMs = System.currentTimeMillis()
            )
            val result = repository.submitReport(report)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    submitFinished = result.isSuccess,
                    lastError = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
