package io.github.jan.supabase.gotrue.exception

/**
 * Exception thrown on sign-up if the password is too weak
 * @param description The description of the exception.
 * @param reasons The reasons why the password is weak.
 */
class AuthWeakPasswordException(
    description: String,
    val reasons: List<String>
) : AuthRestException(
    CODE,
    description,
) {

    internal companion object {
        const val CODE = "weak_password"
    }

}
