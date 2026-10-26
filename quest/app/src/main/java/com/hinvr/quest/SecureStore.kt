package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small string store whose values are sealed with an AES-GCM key that never leaves the
 * Android Keystore. Each value is bound to its own key name, so values can't be swapped.
 */
class SecureStore(context: Context, fileName: String, private val alias: String) {
    private val prefs: SharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    private val key: SecretKey? by lazy {
        runCatching { loadKey() ?: createKey() }
            .onFailure { Log.e(Tag, "Keystore unavailable", it) }
            .getOrNull()
    }

    fun get(name: String): String? {
        val sealed = prefs.getString(name, null) ?: return null
        val secret = key ?: return null
        return runCatching {
            val (iv, body) = sealed.split(':', limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
            val cipher = Cipher.getInstance(Transform)
            cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv))
            cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.onFailure { Log.w(Tag, "Couldn't open $name; treating it as unset") }.getOrNull()
    }

    /** False when the Keystore refused; nothing is written in the clear. */
    fun put(name: String, value: String): Boolean {
        val secret = key ?: return false
        return runCatching {
            val cipher = Cipher.getInstance(Transform)
            cipher.init(Cipher.ENCRYPT_MODE, secret)
            cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
            val body = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val sealed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(body, Base64.NO_WRAP)
            prefs.edit { putString(name, sealed) }
        }.onFailure { Log.e(Tag, "Couldn't seal $name", it) }.isSuccess
    }

    fun remove(name: String) {
        prefs.edit { remove(name) }
    }

    private fun loadKey(): SecretKey? {
        val store = KeyStore.getInstance(Provider).apply { load(null) }
        return (store.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Provider)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val Tag = "HinvrSecureStore"
        const val Provider = "AndroidKeyStore"
        const val Transform = "AES/GCM/NoPadding"
    }
}
