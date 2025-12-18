// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Ink Package
// InkPackage.kt
//
// React Native package registration for InkCanvasViewManager and InkEditorViewManager.

package com.rnretrofrontend.ink

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class InkPackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return emptyList()
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return listOf(
            InkCanvasViewManager(reactContext),
            InkEditorViewManager(reactContext)
        )
    }
}
