package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(
  val uid: String,
  val displayName: String,
  val email: String? = null,
  val handle: String? = null,
)

class AccountHistoryStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun recordAccount(
    uid: String,
    displayName: String?,
    email: String?,
    handle: String? = null,
  ) {
    if (uid.isBlank()) return
    val existing = loadAccounts().filterNot { it.uid == uid }
    val previous = loadAccounts().firstOrNull { it.uid == uid }
    val resolvedName =
      displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: previous?.displayName?.takeIf { it.isNotBlank() }
        ?: "ErtTube Creator"
    val resolvedEmail = email?.trim()?.takeIf { it.isNotEmpty() } ?: previous?.email
    val resolvedHandle = handle?.trim()?.takeIf { it.isNotEmpty() } ?: previous?.handle

    val updated =
      listOf(
        SavedAccount(
          uid = uid,
          displayName = resolvedName,
          email = resolvedEmail,
          handle = resolvedHandle,
        )
      ) + existing
    saveAccounts(updated.take(MAX_ACCOUNTS))
  }

  fun getOtherAccounts(activeUid: String): List<SavedAccount> {
    return loadAccounts().filter { it.uid != activeUid }
  }

  fun getAllAccounts(): List<SavedAccount> = loadAccounts()

  fun removeAccount(uid: String) {
    saveAccounts(loadAccounts().filterNot { it.uid == uid })
  }

  private fun loadAccounts(): List<SavedAccount> {
    val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
    return try {
      val array = JSONArray(raw)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.optJSONObject(i) ?: continue
          val uid = obj.optString("uid", "").trim()
          if (uid.isEmpty()) continue
          val displayName = obj.optString("displayName", "ErtTube Creator").ifBlank { "ErtTube Creator" }
          val email = obj.optString("email", "").ifBlank { null }
          val handle = obj.optString("handle", "").ifBlank { null }
          add(SavedAccount(uid = uid, displayName = displayName, email = email, handle = handle))
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveAccounts(accounts: List<SavedAccount>) {
    val array = JSONArray()
    accounts.forEach { account ->
      val obj =
        JSONObject().apply {
          put("uid", account.uid)
          put("displayName", account.displayName)
          account.email?.let { put("email", it) }
          account.handle?.let { put("handle", it) }
        }
      array.put(obj)
    }
    prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
  }

  private companion object {
    const val PREFS_NAME = "erttube_account_history"
    const val KEY_ACCOUNTS = "saved_accounts_json"
    const val MAX_ACCOUNTS = 8
  }
}
