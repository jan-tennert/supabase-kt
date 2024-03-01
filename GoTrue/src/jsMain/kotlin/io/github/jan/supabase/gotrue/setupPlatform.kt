package io.github.jan.supabase.gotrue

import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_BROWSER
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL

@SupabaseInternal
actual fun Auth.setupPlatform() {
    this as AuthImpl

    fun checkForHash() {
        if(window.location.hash.isBlank()) return

        val afterHash = window.location.hash.substring(1)

        if(!afterHash.contains('=')) {
            // No params after hash, no need to continue
            return
        }

        parseFragmentAndImportSession(afterHash) {
            val newURL = window.location.href.split("?")[0];
            window.history.replaceState({}, window.document.title, newURL);
        }
    }

    fun checkForPCKECode() {
        val url = URL(window.location.href)
        val code = url.searchParams.get("code") ?: return
        authScope.launch {
            val session = exchangeCodeForSession(code)
            importSession(session, source = SessionSource.External)
        }
        val newURL = window.location.href.split("?")[0];
        window.history.replaceState({}, window.document.title, newURL);
    }

    if(IS_BROWSER) {
        window.onhashchange = {
            checkForHash()
        }
        window.onload = {
            checkForHash()
            checkForPCKECode()
        }
    }
}