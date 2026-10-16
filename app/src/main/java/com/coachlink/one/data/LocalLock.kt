package com.coachlink.one.data

import android.content.Context
import android.os.SystemClock
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN protects this device's UI. This is NOT cloud authentication. */
class LocalLock(context: Context) {
    private val prefs = context.getSharedPreferences("coachlink-one-local-lock", Context.MODE_PRIVATE)
    private var failed = 0
    private var blockedUntil = 0L
    val configured: Boolean get() = prefs.contains("hash")
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(),salt,150_000,256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun create(pin: String): Boolean {
        if(configured || !pin.matches(Regex("[0-9]{6}"))) return false
        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("salt",Base64.getEncoder().encodeToString(salt))
            .putString("hash",Base64.getEncoder().encodeToString(derive(pin,salt))).apply()
        return true
    }
    fun check(pin: String): Boolean {
        if (!configured || SystemClock.elapsedRealtime() < blockedUntil) return false
        val salt = runCatching { Base64.getDecoder().decode(prefs.getString("salt","")!!) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(prefs.getString("hash","")!!) }.getOrNull() ?: return false
        val ok = java.security.MessageDigest.isEqual(expected,derive(pin,salt))
        if(ok) { failed=0; blockedUntil=0L }
        else { failed++; if(failed>=5) {blockedUntil=SystemClock.elapsedRealtime()+60_000; failed=0} }
        return ok
    }
}
