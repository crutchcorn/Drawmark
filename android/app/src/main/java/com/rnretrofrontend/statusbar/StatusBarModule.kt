// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Status Bar Module
// StatusBarModule.kt
//
// Provides functionality to expand the notification panel programmatically.

package com.rnretrofrontend.statusbar

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*

private const val TAG = "StatusBarModule"

class StatusBarModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "StatusBarModule"

    /**
     * Expands the notification panel (status bar).
     * Uses reflection to access the StatusBarManager service.
     */
    @SuppressLint("WrongConstant")
    @ReactMethod
    fun expandNotificationPanel(promise: Promise) {
        try {
            val statusBarService = reactApplicationContext.getSystemService("statusbar")
            if (statusBarService != null) {
                val statusBarManager = statusBarService.javaClass
                val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
                expandMethod.invoke(statusBarService)
                promise.resolve(true)
                Log.i(TAG, "Notification panel expanded")
            } else {
                promise.reject("E_SERVICE_NOT_FOUND", "StatusBar service not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error expanding notification panel", e)
            promise.reject("E_EXPAND_FAILED", "Failed to expand notification panel: ${e.message}")
        }
    }

    /**
     * Expands the quick settings panel.
     * Uses reflection to access the StatusBarManager service.
     */
    @SuppressLint("WrongConstant")
    @ReactMethod
    fun expandQuickSettings(promise: Promise) {
        try {
            val statusBarService = reactApplicationContext.getSystemService("statusbar")
            if (statusBarService != null) {
                val statusBarManager = statusBarService.javaClass
                val expandMethod = statusBarManager.getMethod("expandSettingsPanel")
                expandMethod.invoke(statusBarService)
                promise.resolve(true)
                Log.i(TAG, "Quick settings panel expanded")
            } else {
                promise.reject("E_SERVICE_NOT_FOUND", "StatusBar service not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error expanding quick settings panel", e)
            promise.reject("E_EXPAND_FAILED", "Failed to expand quick settings: ${e.message}")
        }
    }
}
