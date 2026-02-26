package ru.rostov

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("7e941bb8-2f22-4a15-9338-51f2153d6ea6")

    }

}
