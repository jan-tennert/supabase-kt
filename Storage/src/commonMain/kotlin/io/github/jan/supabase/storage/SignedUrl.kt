package io.github.jan.supabase.storage

import kotlinx.serialization.Serializable

/**
 * Represents a signed url
 * @param error An optional error message
 * @param signedURL The signed url
 * @param path The path of the file
 */
@Serializable
data class SignedUrl(val error: String?, val signedURL: String, val path: String)
