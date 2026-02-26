package ru.rostov

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vk.id.VKID
import com.vk.id.onetap.xml.OneTapBottomSheet
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
            startActivity(Intent(this@MainActivity, MainPage::class.java))
        }
        setupEdgeToEdge()
        next()
    }

    private fun next() {
        val buttonNext = findViewById<LinearLayout>(R.id.linearLayout2)
        val root = findViewById<View>(R.id.main)
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
        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = ime.bottom.coerceAtLeast(systemBars.bottom)

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                bottomPadding
            )
            insets
        }
    }
}