package com.example.volunteering.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GeocodingService(private val context: Context) {

    suspend fun getAddressSuggestions(query: String): List<Address> {
        if (query.isBlank()) return emptyList()
        val geocoder = Geocoder(context)

        return try {
            val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 15) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 15) ?: emptyList()
            }

            results
                .distinctBy { "${it.featureName}_${it.thoroughfare}_${it.locality}" }
                .sortedByDescending { address ->
                    val hasName = address.featureName != null &&
                            !address.featureName.matches(Regex("^\\d+$"))
                    if (hasName) 1 else 0
                }
                .take(8)

        } catch (e: Exception) {
            Log.e("GeocodingService", "Error getting suggestions", e)
            emptyList()
        }
    }
}