package com.energy.app

import android.app.Application
import com.energy.app.di.AppContainer

class EnergyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
