package com.amacezing.sales.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitHelper {
    fun initFirebaseIfEmpty(context: Context) {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            val dbIdRes = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
            val dbId = if (dbIdRes != 0) {
                try {
                    appContext.getString(dbIdRes)
                } catch (_: Exception) {
                    "ai-studio-android-amacezin-524a1ad7-2303-48b3-b183-252ff1649bea"
                }
            } else {
                "ai-studio-android-amacezin-524a1ad7-2303-48b3-b183-252ff1649bea"
            }
            val projectId = if (dbId.contains("-")) dbId.substringBeforeLast("-") else dbId

            try {
                FirebaseApp.initializeApp(
                    appContext,
                    FirebaseOptions.Builder()
                        .setApplicationId(appContext.packageName)
                        .setProjectId(projectId)
                        .setApiKey("AIzaSy-placeholder-key")
                        .build()
                )
                Log.d("FirebaseInitHelper", "Initialized fallback FirebaseApp for project $projectId")
            } catch (e: Exception) {
                Log.w("FirebaseInitHelper", "FirebaseApp initialization failed: ${e.message}")
            }
        }
    }
}
