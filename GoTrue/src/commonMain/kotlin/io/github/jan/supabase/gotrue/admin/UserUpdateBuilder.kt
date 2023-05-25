package io.github.jan.supabase.gotrue.admin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A builder for updating a user.
 * @property email The user's email address
 * @property password The user's password
 * @property appMetadata Extra app metadata
 * @property userMetadata Extra user metadata
 * @property emailConfirm Automatically confirms the email address
 * @property phoneConfirm Automatically confirms the phone number
 * @property phone The user's phone number
 */
@Serializable
data class UserUpdateBuilder(
    var email: String? = null,
    var password: String? = null,
    @SerialName("app_metadata")
    var appMetadata: JsonObject? = null,
    @SerialName("user_metadata")
    var userMetadata: JsonObject? = null,
    @SerialName("email_confirm")
    var emailConfirm: Boolean? = null,
    @SerialName("phone_confirm")
    var phoneConfirm: Boolean? = null,
    var phone: String? = null
)
