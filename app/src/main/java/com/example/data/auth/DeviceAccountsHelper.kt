package com.example.data.auth

import android.accounts.AccountManager
import android.content.Context
import java.io.Serializable

data class DeviceGoogleAccount(
    val name: String,
    val email: String,
    val avatarUrl: String = ""
) : Serializable

class DeviceAccountsHelper(private val context: Context) {
    fun getAvailableDeviceAccounts(): List<DeviceGoogleAccount> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.map { account ->
                DeviceGoogleAccount(
                    name = account.name.substringBefore("@"),
                    email = account.name,
                    avatarUrl = ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

