package com.adhan.app.infra

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRouterHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: com.adhan.app.domain.LogRepository
) {
    // Inject a specific scope if possible, or use GlobalScope for fire-and-forget logs 
    // BUT simplest is just using android.util.Log for internal infra stuff to avoid complexity, 
    // OR create a non-suspend helper in LogRepository.
    // For this quick fix, I'll switch to standard Logging or fire-and-forget scope.
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun log(message: String) {
        scope.launch { logRepository.log(message) }
    }

    private var mediaRouter: MediaRouter? = null
    private val _availableRoutes = MutableStateFlow<List<RouteInfo>>(emptyList())
    val availableRoutes: StateFlow<List<RouteInfo>> = _availableRoutes

    val selector: MediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_LIVE_AUDIO)
        .addControlCategory(androidx.mediarouter.media.MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    private val callback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes(router)
        }

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes(router)
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes(router)
        }
    }

    // Initialize on main thread if needed, or lazily
    fun init() {
        try {
            if (mediaRouter == null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    mediaRouter = MediaRouter.getInstance(context)
                }
            }
        } catch (e: Exception) {
            log("MediaRouter init failed: ${e.message}")
        }
    }

    fun startScanning() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                mediaRouter?.let { router ->
                    router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
                    updateRoutes(router)
                    log("MediaRouter: Scanning started")
                } ?: run {
                    init() 
                }
            } catch (e: Exception) {
                log("MediaRouter startScanning failed: ${e.message}")
            }
        }
    }

    fun stopScanning() {
         android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                mediaRouter?.removeCallback(callback)
            } catch (e: Exception) { /* ignore */ }
        }
    }
    
    fun selectRoute(routeId: String) {
         android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val route = mediaRouter?.routes?.find { it.id == routeId }
                if (route != null) {
                    log("MediaRouter: Selecting route ${route.name}")
                    route.select()
                } else {
                    log("MediaRouter: Route $routeId not found")
                }
            } catch (e: Exception) {
                 log("MediaRouter selectRoute error: ${e.message}")
            }
        }
    }

    fun selectRouteByName(routeName: String) {
         android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                // If router is null, try init
                if (mediaRouter == null) init()
                
                val route = mediaRouter?.routes?.find { it.name == routeName }
                if (route != null) {
                    log("MediaRouter: Selecting route by name: $routeName")
                    route.select()
                } else {
                    log("MediaRouter: Route with name '$routeName' not found.")
                }
            } catch (e: Exception) {
                 log("MediaRouter selectRouteByName error: ${e.message}")
            }
        }
    }

    private fun updateRoutes(router: MediaRouter) {
        val routes = router.routes.filter { !it.isDefault && !it.isBluetooth }.map { 
             RouteInfo(it.id, it.name, it.description ?: "")
        }
        _availableRoutes.value = routes
    }
    
    data class RouteInfo(val id: String, val name: String, val description: String)
}
