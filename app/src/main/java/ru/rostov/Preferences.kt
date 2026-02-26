package ru.rostov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vk.id.VKID

class Preferences : AppCompatActivity() {
    private var selectedViewId: Int? = null
    private var myVKID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myVKID = VKID.instance.accessToken?.idToken.toString()
    }


    // Логика выбора блока
    /*private fun selectView(selectedView: View, allViews: List<View>) {
        selectedViewId = selectedView.id

        allViews.forEach { view ->
            // Подсветка выбранного
            if (view.id == selectedViewId) {
                view.setBackgroundResource(R.drawable.chosen_block) // выбранный фон
            } else {
                when (view.id) { // возвращаем исходные фоны
                    R.id.parks_container -> view.setBackgroundResource(R.drawable.green_block)
                    R.id.memorial_container -> view.setBackgroundResource(R.drawable.orange_block)
                    R.id.events_container -> view.setBackgroundResource(R.drawable.red_block)
                    R.id.all_container -> view.setBackgroundResource(R.drawable.button_next)
                }
            }
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
    }*/

}