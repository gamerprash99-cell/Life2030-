package com.lifeos.app.core.location

/**
 * The location capability the diary composer needs, expressed without any
 * Android type so the composer's location handling can be tested on a plain JVM.
 * The real implementation ([DeviceLocationProvider]) needs a `Context` and the
 * platform `LocationManager`/`Geocoder`.
 */
interface LocationProvider {
    /** True when either location permission is currently granted. */
    fun hasLocationPermission(): Boolean

    /**
     * Best current fix, or a typed [LocationOutcome.Failure] explaining why not.
     * Never fabricates a position.
     */
    suspend fun currentPlace(): LocationOutcome
}
