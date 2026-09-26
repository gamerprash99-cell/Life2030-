package com.lifeos.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lifeos.app.domain.model.DiaryAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Obtains a real device location for a diary entry using only the Android
 * platform — `LocationManager` for the fix and the platform `Geocoder` for the
 * human-readable place name. No Google Play Services dependency, no network
 * client and no third-party SDK is involved, which keeps the app's
 * offline-first, privacy-first guarantees intact.
 *
 * Nothing here ever runs at app launch: the Diary screen calls
 * [currentPlace] only after the user taps "Add location" and has granted
 * location permission, so the consent prompt is always user-initiated.
 *
 * Coordinates are used solely to render a place name for that entry and are
 * stored encrypted in the local Room database. They are never transmitted.
 */
class DeviceLocationProvider(private val context: Context) {

    private val locationManager: LocationManager? =
        ContextCompat.getSystemService(context, LocationManager::class.java)

    /** True when either location permission is currently granted. */
    fun hasLocationPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Best current fix, or a typed [LocationOutcome.Failure] explaining why not.
     *
     * Provider selection mirrors what the user would expect: GPS when it is
     * enabled, otherwise the network provider, otherwise the last known fix
     * from any provider. Nothing is fabricated when all of them are unavailable.
     */
    suspend fun currentPlace(): LocationOutcome = withContext(Dispatchers.IO) {
        val manager = locationManager
            ?: return@withContext LocationOutcome.Failure(LocationFailure.NO_PROVIDER)

        if (!hasLocationPermission()) {
            return@withContext LocationOutcome.Failure(LocationFailure.PERMISSION_DENIED)
        }

        val providers = runCatching { manager.getProviders(true) }.getOrElse { emptyList() }
        if (providers.isEmpty()) {
            return@withContext LocationOutcome.Failure(LocationFailure.PROVIDERS_DISABLED)
        }

        val fix = resolveFix(manager, providers)
            ?: return@withContext LocationOutcome.Failure(LocationFailure.NO_FIX)

        LocationOutcome.Success(
            place = DiaryAttachment.Place(
                latitude = fix.latitude,
                longitude = fix.longitude,
                // Left empty here; filled by resolvePlaceName below when a
                // geocoder backend exists. Never a hardcoded city.
                placeName = "",
                accuracyMeters = if (fix.hasAccuracy()) fix.accuracy else null
            ),
            placeName = resolvePlaceName(fix)
        )
    }

    private fun resolveFix(manager: LocationManager, providers: List<String>): Location? {
        // Prefer an explicitly enabled GPS fix, then network, then any provider.
        val ordered = buildList {
            if (LocationManager.GPS_PROVIDER in providers) add(LocationManager.GPS_PROVIDER)
            if (LocationManager.NETWORK_PROVIDER in providers) add(LocationManager.NETWORK_PROVIDER)
            addAll(providers)
        }.distinct()

        // Coarse is enough for a place name, so ask for coarse explicitly: a
        // user who granted only coarse access still gets a real fix rather than
        // a permission error. GPS needs fine, so it is only read when granted.
        val canReadGps = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        for (provider in ordered) {
            if (provider == LocationManager.GPS_PROVIDER && !canReadGps) continue
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) continue
            val fresh = try {
                manager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                // Permission was revoked between the check and the call.
                null
            } catch (_: IllegalArgumentException) {
                // Provider disappeared from the device since getProviders().
                null
            }
            if (fresh != null) return fresh
        }
        return null
    }

    /**
     * Resolves coordinates to a human-readable place using the platform
     * `Geocoder`. Returns an empty string when no geocoder backend is present
     * (common on devices without Google Play services) — the UI then shows the
     * coordinates instead of inventing a city name.
     */
    private suspend fun resolvePlaceName(location: Location): String {
        if (!Geocoder.isPresent()) return ""
        return try {
            // Bounded so a slow/absent geocoder backend can never hang the UI.
            withTimeout(GEOCODER_TIMEOUT_MILLIS) {
                geocoderAddresses(location)
            }.firstOrNull { it.isNotBlank() }.orEmpty()
        } catch (_: TimeoutCancellationException) {
            ""
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocoderAddresses(location: Location): List<String> {
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    MAX_GEOCODER_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (continuation.isActive) continuation.resume(addresses.map { it.getAddressLine(0).orEmpty() })
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                )
            }
        } else {
            // Pre-Tiramisu: the blocking API is safe here because we are already
            // on Dispatchers.IO and wrapped in withTimeout above.
            @Suppress("DEPRECATION")
            runCatching {
                geocoder.getFromLocation(location.latitude, location.longitude, MAX_GEOCODER_RESULTS)
                    ?.map { it.getAddressLine(0).orEmpty() }
                    .orEmpty()
            }.getOrElse { emptyList() }
        }
    }

    private companion object {
        const val GEOCODER_TIMEOUT_MILLIS = 4_000L
        const val MAX_GEOCODER_RESULTS = 1
    }
}

/** Why a location could not be attached. Drives the honest empty/error states. */
enum class LocationFailure { PERMISSION_DENIED, NO_PROVIDER, PROVIDERS_DISABLED, NO_FIX }

/** Result of a location request. Never carries a fabricated coordinate. */
sealed interface LocationOutcome {
    data class Success(
        val place: DiaryAttachment.Place,
        val placeName: String
    ) : LocationOutcome

    data class Failure(val reason: LocationFailure) : LocationOutcome
}
