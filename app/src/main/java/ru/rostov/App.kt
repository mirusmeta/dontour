package ru.rostov

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.common.logger.DefaultLogger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("0d179806-09da-46e6-ac02-2bff3bc930e8")
        MapKitFactory.initialize(this)
        RuStorePushClient.init(
            application = this,
            projectId = "YItQMl6LwpxGTsKDAtL0JGP0wAfnvUNC",
            logger = DefaultLogger()
        )
    }

}
