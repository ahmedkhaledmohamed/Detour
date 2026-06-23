package com.ahmedkhaled.onroute.service

import com.ahmedkhaled.onroute.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetourRoute(
    val points: List<LatLng>,
    val totalDurationSeconds: Int
)

class DirectionsService {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun getDetourRoute(
        origin: LatLng,
        waypoint: LatLng,
        destination: LatLng,
        travelMode: String = "driving"
    ): DetourRoute? = withContext(Dispatchers.IO) {
        val mode = when (travelMode.uppercase()) {
            "DRIVE" -> "driving"
            "WALK" -> "walking"
            "BICYCLE" -> "bicycling"
            else -> "driving"
        }
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&waypoints=${waypoint.latitude},${waypoint.longitude}" +
            "&mode=$mode" +
            "&key=${BuildConfig.MAPS_API_KEY}"

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = moshi.adapter(Map::class.java).fromJson(body) as? Map<*, *> ?: return@withContext null
            val routes = json["routes"] as? List<*> ?: return@withContext null
            val route = routes.firstOrNull() as? Map<*, *> ?: return@withContext null

            val overviewPolyline = route["overview_polyline"] as? Map<*, *>
            val encodedPoints = overviewPolyline?.get("points") as? String ?: return@withContext null
            val points = PolyUtil.decode(encodedPoints)

            val legs = route["legs"] as? List<*> ?: emptyList<Any>()
            var totalDuration = 0
            for (leg in legs) {
                val legMap = leg as? Map<*, *> ?: continue
                val duration = legMap["duration"] as? Map<*, *>
                val value = duration?.get("value") as? Double
                totalDuration += value?.toInt() ?: 0
            }

            DetourRoute(points = points, totalDurationSeconds = totalDuration)
        } catch (e: Exception) {
            null
        }
    }
}
