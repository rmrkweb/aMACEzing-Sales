package com.amacezing.sales

import android.app.Application
import com.amacezing.sales.util.FirebaseInitHelper

class SalesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitHelper.initFirebaseIfEmpty(this)
    }
}
