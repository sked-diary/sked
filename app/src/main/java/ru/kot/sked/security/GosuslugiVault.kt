package ru.kot.sked.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class StoredAuthData(
    val login: String,
    val passEncrypted: String,
    val passIv: String,
    val has2fa: Boolean,
    val totpSecretEncrypted: String?,
    val totpIv: String?,
    val pinHash: String,
    val pinEncrypted: String? = null,
    val pinIv: String? = null
)

object GosuslugiVault {
    private const val KEY_ALIAS = "GosuslugiVaultKey_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "gosuslugi_vault_prefs"
    private const val KEY_AUTH_DATA = "auth_data"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(encryptedBase64: String, ivBase64: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val decrypted = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
        return String(decrypted, Charsets.UTF_8)
    }

    private fun hashPin(pin: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(("sked_salt_" + pin).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun hasCredentials(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_AUTH_DATA)
    }

    fun saveCredentials(
        context: Context,
        login: String,
        pass: String,
        has2fa: Boolean,
        totpSecret: String?,
        pin: String
    ): Boolean {
        return try {
            val (passEnc, passIv) = encrypt(pass)
            var totpEnc: String? = null
            var totpIv: String? = null
            if (has2fa && !totpSecret.isNullOrBlank()) {
                val cleanTotp = totpSecret.replace(" ", "").uppercase()
                val (tEnc, tIv) = encrypt(cleanTotp)
                totpEnc = tEnc
                totpIv = tIv
            }

            val (pinEnc, pinIv) = encrypt(pin)

            val data = StoredAuthData(
                login = login.trim(),
                passEncrypted = passEnc,
                passIv = passIv,
                has2fa = has2fa,
                totpSecretEncrypted = totpEnc,
                totpIv = totpIv,
                pinHash = hashPin(pin),
                pinEncrypted = pinEnc,
                pinIv = pinIv
            )

            val json = Gson().toJson(data)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AUTH_DATA, json)
                .apply()

            // Also enable and set the app lock PIN in mainPrefs immediately
            context.getSharedPreferences("main", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_pin", true)
                .putString("pin", pin)
                .putBoolean("first_launch", false)
                .putBoolean("pin_disabled_manually", false)
                .apply()
            true
        } catch (e: Exception) {
            Log.e("GosuslugiVault", "Error saving credentials", e)
            false
        }
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AUTH_DATA, null) ?: return false
        return try {
            val data = Gson().fromJson(json, StoredAuthData::class.java)
            data.pinHash == hashPin(pin)
        } catch (e: Exception) {
            false
        }
    }

    fun getDecryptedPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AUTH_DATA, null) ?: return null
        return try {
            val data = Gson().fromJson(json, StoredAuthData::class.java)
            if (data.pinEncrypted.isNullOrEmpty() || data.pinIv.isNullOrEmpty()) {
                null
            } else {
                decrypt(data.pinEncrypted, data.pinIv)
            }
        } catch (e: Exception) {
            Log.e("GosuslugiVault", "Error decrypting pin", e)
            null
        }
    }

    fun getDecryptedCredentials(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AUTH_DATA, null) ?: return null
        return try {
            val data = Gson().fromJson(json, StoredAuthData::class.java)
            val pass = decrypt(data.passEncrypted, data.passIv)
            Pair(data.login, pass)
        } catch (e: Exception) {
            Log.e("GosuslugiVault", "Error decrypting credentials", e)
            null
        }
    }

    fun getDecryptedTotpSecret(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AUTH_DATA, null) ?: return null
        return try {
            val data = Gson().fromJson(json, StoredAuthData::class.java)
            if (!data.has2fa || data.totpSecretEncrypted == null || data.totpIv == null) {
                null
            } else {
                decrypt(data.totpSecretEncrypted, data.totpIv)
            }
        } catch (e: Exception) {
            Log.e("GosuslugiVault", "Error decrypting totp secret", e)
            null
        }
    }

    fun has2fa(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_AUTH_DATA, null) ?: return false
        return try {
            val data = Gson().fromJson(json, StoredAuthData::class.java)
            data.has2fa
        } catch (e: Exception) {
            false
        }
    }

    fun clearCredentials(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUTH_DATA)
            .apply()
    }

    // RFC 6238 Standard TOTP Generation
    fun generateCurrentTotp(secretBase32: String, timeWindowSec: Long = 30): String? {
        return try {
            val clean = secretBase32.replace(" ", "").replace("-", "").uppercase()
            val key = decodeBase32(clean)
            val time = (System.currentTimeMillis() / 1000) / timeWindowSec
            val data = ByteBuffer.allocate(8).putLong(time).array()

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(key, "HmacSHA1"))
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 1000000
            String.format("%06d", otp)
        } catch (e: Exception) {
            Log.e("GosuslugiVault", "Failed to generate TOTP", e)
            null
        }
    }

    private fun decodeBase32(base32: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in base32) {
            if (c == '=') break
            val value = alphabet.indexOf(c)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
