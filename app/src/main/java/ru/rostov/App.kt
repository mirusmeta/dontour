package ru.rostov

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("0d179806-09da-46e6-ac02-2bff3bc930e8")
        MapKitFactory.initialize(this)
    }

}
