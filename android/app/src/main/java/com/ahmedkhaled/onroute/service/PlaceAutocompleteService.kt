package com.ahmedkhaled.onroute.service

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

data class PlaceSuggestion(
    val placeId: String,
    val title: String,
    val subtitle: String
)

class PlaceAutocompleteService(private val placesClient: PlacesClient) {

    suspend fun getSuggestions(query: String): List<PlaceSuggestion> {
        if (query.length < 2) return emptyList()

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            response.autocompletePredictions.map { prediction ->
                PlaceSuggestion(
                    placeId = prediction.placeId,
                    title = prediction.getPrimaryText(null).toString(),
                    subtitle = prediction.getSecondaryText(null).toString()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun resolvePlace(placeId: String): Pair<LatLng, String>? {
        val fields = listOf(Place.Field.LOCATION, Place.Field.DISPLAY_NAME)
        val request = FetchPlaceRequest.newInstance(placeId, fields)

        return try {
            val response = placesClient.fetchPlace(request).await()
            val place = response.place
            val latLng = place.location ?: return null
            val name = place.displayName ?: ""
            Pair(latLng, name)
        } catch (e: Exception) {
            null
        }
    }
}
