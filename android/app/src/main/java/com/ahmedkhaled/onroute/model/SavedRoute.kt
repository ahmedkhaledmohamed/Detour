package com.ahmedkhaled.onroute.model

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class SavedRoute(
    val id: String,
    val name: String,
    val originLat: Double,
    val originLng: Double,
    val originName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationName: String,
    val defaultCategory: String,
    val createdAt: Long
) {
    val originLatLng: LatLng get() = LatLng(originLat, originLng)
    val destinationLatLng: LatLng get() = LatLng(destinationLat, destinationLng)
}

class SavedRoutesStore(context: Context) {
    private val prefs = context.getSharedPreferences("saved_routes", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, SavedRoute::class.java)
    private val adapter = moshi.adapter<List<SavedRoute>>(type)
    private val maxRoutes = 5

    fun load(): List<SavedRoute> {
        val json = prefs.getString("routes", null) ?: return emptyList()
        return try { adapter.fromJson(json) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun save(route: SavedRoute) {
        val routes = load().toMutableList()
        routes.removeAll { it.id == route.id }
        routes.add(0, route)
        val trimmed = routes.take(maxRoutes)
        prefs.edit().putString("routes", adapter.toJson(trimmed)).apply()
    }

    fun delete(route: SavedRoute) {
        val routes = load().toMutableList()
        routes.removeAll { it.id == route.id }
        prefs.edit().putString("routes", adapter.toJson(routes)).apply()
    }
}

@JsonClass(generateAdapter = true)
data class RecentSearch(
    val id: String,
    val originName: String,
    val originLat: Double,
    val originLng: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val category: String,
    val timestamp: Long
) {
    val originLatLng: LatLng get() = LatLng(originLat, originLng)
    val destinationLatLng: LatLng get() = LatLng(destinationLat, destinationLng)
}

class RecentSearchStore(context: Context) {
    private val prefs = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, RecentSearch::class.java)
    private val adapter = moshi.adapter<List<RecentSearch>>(type)
    private val maxEntries = 10

    fun load(): List<RecentSearch> {
        val json = prefs.getString("searches", null) ?: return emptyList()
        return try { adapter.fromJson(json) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun add(originName: String, origin: LatLng, destinationName: String, destination: LatLng, category: String) {
        val entries = load().toMutableList()
        entries.removeAll { it.originName == originName && it.destinationName == destinationName && it.category == category }
        entries.add(0, RecentSearch(
            id = java.util.UUID.randomUUID().toString(),
            originName = originName, originLat = origin.latitude, originLng = origin.longitude,
            destinationName = destinationName, destinationLat = destination.latitude, destinationLng = destination.longitude,
            category = category, timestamp = System.currentTimeMillis()
        ))
        val trimmed = entries.take(maxEntries)
        prefs.edit().putString("searches", adapter.toJson(trimmed)).apply()
    }
}
