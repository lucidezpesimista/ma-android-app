package com.ma.app

import android.app.Application

/**
 * Application class para Ma.
 * 
 * Por ahora no requiere inicialización especial, pero está preparada
 * para futuras extensiones (como WorkManager, Analytics, etc.)
 */
class MaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicialización global si es necesaria
    }
}
