// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Status Bar Package
// StatusBarPackage.kt
//
// React Native package registration for StatusBarModule.

package com.rnretrofrontend.statusbar

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class StatusBarPackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(StatusBarModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
