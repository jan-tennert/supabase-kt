package io.github.jan.supabase.gotrue

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import co.touchlab.kermit.Logger
import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private var appContext: Context? = null

internal class SupabaseInitializer : Initializer<Context> {
    override fun create(context: Context): Context = context.applicationContext.also { appContext = it }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}

internal fun applicationContext(): Context = appContext ?: error("Application context not initialized")

@SupabaseInternal
actual fun Auth.setupPlatform() {
    addLifecycleCallbacks(this)
}

private fun addLifecycleCallbacks(gotrue: Auth) {
    if(!gotrue.config.enableLifecycleCallbacks) return
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    gotrue as AuthImpl
    val scope = gotrue.authScope
    scope.launch(Dispatchers.Main) {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {

                override fun onStart(owner: LifecycleOwner) {
                    if(!gotrue.isAutoRefreshRunning && gotrue.config.alwaysAutoRefresh) {
                        Logger.d("Auth") {
                            "Trying to re-load session from storage..."
                        }
                        scope.launch {
                            val sessionFound = gotrue.loadFromStorage()
                            if(!sessionFound) {
                                Logger.d("Auth") {
                                    "No session found, not starting auto refresh"
                                }
                            } else {
                                Logger.d("Auth") {
                                    "Session found, auto refresh started"
                                }
                            }
                        }
                    }
                }
                override fun onStop(owner: LifecycleOwner) {
                    if(gotrue.isAutoRefreshRunning) {
                        Logger.d("Auth") { "Cancelling auto refresh because app is switching to the background" }
                        scope.launch {
                            gotrue.stopAutoRefreshForCurrentSession()
                            gotrue.resetLoadingState()
                        }
                    }
                }
            }
        )
    }
}
