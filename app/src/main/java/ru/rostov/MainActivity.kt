package ru.rostov

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.vk.id.VKID
import com.vk.id.onetap.xml.OneTapBottomSheet
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.common.logger.DefaultLogger
import java.util.jar.Manifest
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    object VKIDHelper {
        var isInitialized = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!VKIDHelper.isInitialized) {
            VKID.init(this@MainActivity)
            VKIDHelper.isInitialized = true
        }

        var sPref = getSharedPreferences("data", MODE_PRIVATE)
        val canGo = sPref.getBoolean("first_meet", false)
        if (canGo){
            startActivity(Intent(this, MainPage::class.java))
        }else{
            setContentView(R.layout.activity_main)
            setupEdgeToEdge()
            next()
        }
        askNotificationPermission()
        RuStorePushClient.checkPushAvailability()
            .addOnSuccessListener { result ->
                Log.d("RuStorePush", "Пуши доступны: $result")
            }
            .addOnFailureListener { e ->
                Log.e("RuStorePush", "Пуши НЕ доступны: ${e.message}")
            }
        // В onCreate
        RuStorePushClient.subscribeToTopic("all_users")
            .addOnSuccessListener {
                Log.d("RuStorePush", "Подписка на 'all_users' оформлена!")
            }
            .addOnFailureListener { e ->
                Log.e("RuStorePush", "Ошибка подписки: ${e.message}")
            }
        RuStorePushClient.getToken()
            .addOnSuccessListener { token ->
                Log.d("RUSTORE_TOKEN", "Мой токен: $token")
            }
            .addOnFailureListener { e ->
                Log.e("RUSTORE_TOKEN", "Ошибка получения токена: ${e.message}")
            }
    }

    private fun next() {
        val buttonNext = findViewById<LinearLayout>(R.id.linearLayout2)
        val root = findViewById<ConstraintLayout>(R.id.main)
        buttonNext.setOnClickListener {
            val vkidOneTapBottomSheet =
                findViewById<OneTapBottomSheet>(R.id.vkid_bottom_sheet)
            vkidOneTapBottomSheet.setCallbacks(
                onAuth = { oAuth, token ->
                    root.animate()
                        .alpha(0f)
                        .translationY(-100f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction {
                            var sPref = getSharedPreferences("data", MODE_PRIVATE).edit()
                            sPref.putBoolean("first_meet", true).apply()
                            val intent = Intent(this, MainPage::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .start()
                }, onFail = { oAuth, fail ->
                    Log.e("Auth", "Ошибка")
                })

            vkidOneTapBottomSheet.show()
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = when {
                ime.bottom > 0 -> ime.bottom
                systemBars.bottom > 100 -> systemBars.bottom
                else -> 0
            }

            v.setPadding(0, 0, 0, bottomPadding)
            insets
        }
    }
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}