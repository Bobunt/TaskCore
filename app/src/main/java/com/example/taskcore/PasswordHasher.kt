package com.example.taskcore

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {

    fun generateSalt(bytes: Int = 16): String {
        val salt = ByteArray(bytes)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, saltBase64: String): String {
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltBytes)
        val digest = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}