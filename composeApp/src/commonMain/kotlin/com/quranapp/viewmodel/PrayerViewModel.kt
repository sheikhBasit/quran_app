package com.quranapp.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.quranapp.domain.model.NextPrayer
import com.quranapp.domain.model.PrayerTimesResult
import com.quranapp.domain.usecase.prayer.GetNextPrayerUseCase
import com.quranapp.domain.usecase.prayer.GetPrayerTimesUseCase
import com.quranapp.util.Coordinates
import com.quranapp.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrayerUiState(
    val times: PrayerTimesResult? = null,
    val nextPrayer: NextPrayer? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PrayerViewModel(
    private val getPrayerTimes: GetPrayerTimesUseCase,
    private val getNextPrayer: GetNextPrayerUseCase,
    private val locationProvider: LocationProvider
) : ScreenModel {
    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        observeLocation()
    }

    private fun observeLocation() {
        screenModelScope.launch {
            locationProvider.getLocationFlow().collect { coords ->
                if (coords != null) {
                    loadPrayerTimes(coords.latitude, coords.longitude)
                } else {
                    _uiState.update { it.copy(
                        error = "Please enable GPS and grant location permission",
                        isLoading = false 
                    ) }
                }
            }
        }
    }

    fun loadPrayerTimes(latitude: Double, longitude: Double) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val times = getPrayerTimes(latitude, longitude).getOrThrow()
                val next = getNextPrayer(latitude, longitude).getOrThrow()
                _uiState.update { it.copy(times = times, nextPrayer = next, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
