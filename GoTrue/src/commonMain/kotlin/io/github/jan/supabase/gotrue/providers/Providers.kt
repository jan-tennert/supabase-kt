package io.github.jan.supabase.gotrue.providers

object Google : OAuthProvider(), IDTokenProvider {

    override val name = "google"

}

object Discord : OAuthProvider() {

    override val name = "discord"

}

object Github : OAuthProvider() {

    override val name = "github"

}

object Gitlab : OAuthProvider() {

    override val name = "gitlab"

}

object Keycloak : OAuthProvider() {

    override val name = "keycloak"

}

object LinkedIn : OAuthProvider() {

    override val name = "linkedin"

}

object Notion : OAuthProvider() {

    override val name = "notion"

}

object Slack : OAuthProvider() {

    override val name = "slack"

}

object Twitch : OAuthProvider() {

    override val name = "twitch"

}

object Twitter : OAuthProvider() {

    override val name = "twitter"

}

object WorkOS : OAuthProvider() {

    override val name = "workos"

}

object Zoom : OAuthProvider() {

    override val name = "zoom"

}

object Bitbucket : OAuthProvider() {

    override val name = "bitbucket"

}

object Azure : OAuthProvider() {

    override val name = "azure"

}

object Apple : OAuthProvider(), IDTokenProvider {

    override val name = "apple"

}

object Spotify : OAuthProvider() {

    override val name = "spotify"

}

operator fun OAuthProvider.Companion.invoke(provider: String) = object : OAuthProvider() {

    override val name = provider

}