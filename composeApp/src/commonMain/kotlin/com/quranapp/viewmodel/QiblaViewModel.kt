package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.usecase.qibla.GetQiblaDirectionUseCase
import com.quranapp.util.CompassSensor
import com.quranapp.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QiblaUiState(
    val direction: Double = 0.0,
    val qiblaBearing: Double = 0.0,
    val error: String? = null
)

class QiblaViewModel(
    private val getQiblaDirection: GetQiblaDirectionUseCase,
    private val locationProvider: LocationProvider,
    private val compassSensor: CompassSensor
) : ScreenModel {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    init {
        observeSensors()
    }

    private fun observeSensors() {
        screenModelScope.launch {
            locationProvider.getLocationFlow().collect { coords ->
                if (coords != null) {
                    val bearing = getQiblaDirection(coords.latitude, coords.longitude)
                    _uiState.update { it.copy(qiblaBearing = bearing, error = null) }
                } else {
                    _uiState.update { it.copy(error = "Please enable GPS and grant location permission") }
                }
            }
        }
        screenModelScope.launch {
            compassSensor.getBearingFlow().collect { bearing ->
                _uiState.update { it.copy(direction = bearing.toDouble()) }
            }
        }
    }
}
