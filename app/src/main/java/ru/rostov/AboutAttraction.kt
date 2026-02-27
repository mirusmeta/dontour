package ru.rostov

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.rostov.database.SupaBaseConfig
import java.net.URLEncoder

class AboutAttraction : AppCompatActivity() {

    private lateinit var placeName: String
    // Ссылка на ваш бакет
    private val storageBaseUrl = "${SupaBaseConfig.supabaseUrl.supabaseUrl}/storage/v1/object/public/Images"

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToSupabase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_attraction)

        placeName = intent.getStringExtra("place_name") ?: "Unknown"
        val photoUrl = intent.getStringExtra("place_photo")

        val titleTv = findViewById<TextView>(R.id.title_tv)
        val imageMain = findViewById<ImageView>(R.id.picture)
        val btnAddPhoto = findViewById<ConstraintLayout>(R.id.add_photo_btn)
        val btnBack = findViewById<ImageView>(R.id.b_back)
        val goToMap = findViewById<TextView>(R.id.goToMap)

        titleTv.text = placeName
        if (!photoUrl.isNullOrEmpty()) {
            Picasso.get().load(photoUrl).into(imageMain)
        }

        btnBack.setOnClickListener { finish() }
        btnAddPhoto.setOnClickListener { pickImage.launch("image/*") }
        goToMap.setOnClickListener {

            val intent = Intent(this, MainPage::class.java).apply {
                putExtra("open_map", true)
                putExtra("map_address", placeName) // лучше адрес
            }

            startActivity(intent)
        }
    }

    private fun uploadImageToSupabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Формируем имя: "Название(1).png" или через timestamp, чтобы избежать дублей
                val fileName = "${placeName}(1).png"
                val encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")

                // Читаем байты файла
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("$storageBaseUrl/$encodedName")
                    .post(bytes.toRequestBody("image/png".toMediaTypeOrNull()))
                    .addHeader("Authorization", "Bearer ${SupaBaseConfig.supabaseKey.supabaseKey}")
                    // Если бакет требует API ключ в заголовке apikey:
                    .addHeader("apikey", SupaBaseConfig.supabaseKey.supabaseKey)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AboutAttraction, "Успешно загружено", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AboutAttraction, "Ошибка: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AboutAttraction, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}