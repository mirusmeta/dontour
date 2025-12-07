package ru.dontour

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
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
            VKID.init(this)
            VKIDHelper.isInitialized = true
        }
        var sPref = getSharedPreferences("data", MODE_PRIVATE)
        val canGo = sPref.getBoolean("first_meet", false)
        if (canGo){
            startActivity(Intent(this@MainActivity, MainPage::class.java))
        }
        animateMain()
        setupEdgeToEdge()
        next()
    }

    private fun next() {
        val buttonNext = findViewById<View>(R.id.button_next)
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
                            val intent = Intent(this, Preferences::class.java)
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

    private fun animateMain() {
        setContentView(R.layout.activity_main)
        val textWelcome = findViewById<TextView>(R.id.text_welcome)
        val textWelcomeMain = findViewById<TextView>(R.id.text_welcome_main)
        val firstContr = findViewById<View>(R.id.first_contr)
        val secondContr = findViewById<View>(R.id.second_contr)
        val thirdConstr = findViewById<View>(R.id.third_constr)
        val additional_text = findViewById<TextView>(R.id.additional_text)
        val additional_icon = findViewById<ImageView>(R.id.additional_icon)
        val root = findViewById<View>(R.id.main)
        val buttonNext = findViewById<View>(R.id.button_next)


        val allViews = listOf(
            textWelcome,
            textWelcomeMain,
            firstContr,
            secondContr,
            thirdConstr,
            additional_text,
            additional_icon,
            buttonNext
        )

        allViews.forEach {
            it.alpha = 0f
        }
        listOf(firstContr, secondContr, thirdConstr, additional_icon, additional_text, buttonNext).forEach {
            it.translationY = 100f
        }
        textWelcome.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        textWelcomeMain.postDelayed({
            textWelcomeMain.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        }, 200)
        val blocks = listOf(firstContr, secondContr, thirdConstr)
        blocks.forEachIndexed { index, block ->
            block.postDelayed({
                block.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }, (index * 200 + 400).toLong())
        }
        additional_icon.postDelayed({
            additional_icon.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }, 1100)
        additional_text.postDelayed({
            additional_text.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }, 1100)
        buttonNext.postDelayed({
            buttonNext.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                .withEndAction {
                    buttonNext.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }, 1300)
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