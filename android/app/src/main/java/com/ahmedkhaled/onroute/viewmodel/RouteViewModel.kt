package com.ahmedkhaled.onroute.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedkhaled.onroute.model.TravelMode
import com.ahmedkhaled.onroute.service.LocationService
import com.ahmedkhaled.onroute.service.PlaceAutocompleteService
import com.ahmedkhaled.onroute.service.PlaceSuggestion
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    var originQuery by mutableStateOf("")
        private set
    var destinationQuery by mutableStateOf("")
        private set
    var originLatLng by mutableStateOf<LatLng?>(null)
        private set
    var destinationLatLng by mutableStateOf<LatLng?>(null)
        private set
    var originName by mutableStateOf<String?>(null)
        private set
    var destinationName by mutableStateOf<String?>(null)
        private set

    var originSuggestions by mutableStateOf<List<PlaceSuggestion>>(emptyList())
        private set
    var destinationSuggestions by mutableStateOf<List<PlaceSuggestion>>(emptyList())
        private set

    var travelMode by mutableStateOf(TravelMode.DRIVE)

    val isSearchReady: Boolean
        get() = originLatLng != null && destinationLatLng != null

    private val placesService = PlaceAutocompleteService(Places.createClient(application))
    val locationService = LocationService(application)

    private var originDebounceJob: Job? = null
    private var destinationDebounceJob: Job? = null

    fun updateOriginQuery(query: String) {
        originQuery = query
        originLatLng = null
        originName = null
        originDebounceJob?.cancel()
        originDebounceJob = viewModelScope.launch {
            delay(300)
            originSuggestions = placesService.getSuggestions(query)
        }
    }

    fun updateDestinationQuery(query: String) {
        destinationQuery = query
        destinationLatLng = null
        destinationName = null
        destinationDebounceJob?.cancel()
        destinationDebounceJob = viewModelScope.launch {
            delay(300)
            destinationSuggestions = placesService.getSuggestions(query)
        }
    }

    fun selectOriginSuggestion(suggestion: PlaceSuggestion) {
        originQuery = suggestion.title
        originSuggestions = emptyList()
        viewModelScope.launch {
            placesService.resolvePlace(suggestion.placeId)?.let { (latLng, name) ->
                originLatLng = latLng
                originName = name.ifEmpty { suggestion.title }
            }
        }
    }

    fun selectDestinationSuggestion(suggestion: PlaceSuggestion) {
        destinationQuery = suggestion.title
        destinationSuggestions = emptyList()
        viewModelScope.launch {
            placesService.resolvePlace(suggestion.placeId)?.let { (latLng, name) ->
                destinationLatLng = latLng
                destinationName = name.ifEmpty { suggestion.title }
            }
        }
    }

    fun useCurrentLocation(latLng: LatLng) {
        originQuery = "Current Location"
        originLatLng = latLng
        originName = "Current Location"
        originSuggestions = emptyList()
    }

    fun swapOriginDestination() {
        val tmpQuery = originQuery
        val tmpLatLng = originLatLng
        val tmpName = originName
        originQuery = destinationQuery
        originLatLng = destinationLatLng
        originName = destinationName
        destinationQuery = tmpQuery
        destinationLatLng = tmpLatLng
        destinationName = tmpName
    }

    fun clearOrigin() {
        originQuery = ""
        originLatLng = null
        originName = null
        originSuggestions = emptyList()
    }

    fun clearDestination() {
        destinationQuery = ""
        destinationLatLng = null
        destinationName = null
        destinationSuggestions = emptyList()
    }
}
