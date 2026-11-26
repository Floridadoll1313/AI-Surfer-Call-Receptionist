package com.example.integrations

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.OceanSurferApp
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

object OwnerCallSession {
  private const val alias = "kai-owner-call-session"
  private val file get() = File(OceanSurferApp.instance.noBackupFilesDir, "owner-session")
  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
      init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
      generateKey()
    }
  }

  @Synchronized
  fun save(session: JSONObject) {
    require(session.getJSONObject("user").getString("id") == "2171c4f4-05bf-4b25-bbdb-2caf8998566a") { "Owner sign-in required" }
    val payload = JSONObject().put("token", session.getString("access_token"))
      .put("expires", System.currentTimeMillis() + session.getLong("expires_in") * 1000)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
    val bytes = cipher.doFinal(payload.toString().toByteArray())
    file.writeText(Base64.encodeToString(cipher.iv + bytes, Base64.NO_WRAP))
  }

  @Synchronized
  fun bearer(project: String): String {
    if (project.trimEnd('/') != KaiOwnerLeadClient.project) throw IOException("Select the AI SURFER project in Integrations.")
    val payload = runCatching {
      val bytes = Base64.decode(file.readText(), Base64.NO_WRAP)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
      JSONObject(String(cipher.doFinal(bytes, 12, bytes.size - 12)))
    }.getOrNull()
    if (payload == null || payload.optLong("expires") <= System.currentTimeMillis() + 5000)
      throw IOException("Sign in to call sync in Integrations.")
    return "Bearer " + payload.getString("token")
  }

  @Synchronized
  fun clear() { file.delete() }
}
