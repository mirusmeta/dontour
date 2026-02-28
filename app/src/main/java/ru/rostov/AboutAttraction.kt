package ru.rostov

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.rostov.database.SupaBaseConfig
import java.net.URLEncoder

class AboutAttraction : AppCompatActivity() {

    private val TAG = "AboutAttractionLog"

    private lateinit var placeId: String
    private lateinit var placeName: String
    private var currentRating: Int = 0

    private val storageBaseUrl = "${SupaBaseConfig.supabaseUrl.supabaseUrl}/storage/v1/object/public/Images"

    private val supabase = createSupabaseClient(
        supabaseUrl = SupaBaseConfig.supabaseUrl.supabaseUrl,
        supabaseKey = SupaBaseConfig.supabaseKey.supabaseKey
    ) { install(Postgrest) }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Log.d(TAG, "Image picker result: $uri")
        uri?.let { uploadImageToSupabase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_attraction)

        // Получаем данные из Intent
        placeId = intent.getStringExtra("place_id") ?: ""
        placeName = intent.getStringExtra("place_name") ?: "Unknown"
        val photoUrl = intent.getStringExtra("place_photo")

        Log.i(TAG, "Activity started. ID: $placeId, Name: $placeName")

        val titleTv = findViewById<TextView>(R.id.title_tv)
        val imageMain = findViewById<ImageView>(R.id.picture)
        val btnAddPhoto = findViewById<ConstraintLayout>(R.id.add_photo_btn)
        val btnPublish = findViewById<ConstraintLayout>(R.id.constraintLayout3)
        val starsContainer = findViewById<LinearLayout>(R.id.linearLayout)
        val btnBack = findViewById<ImageView>(R.id.b_back)
        val goToMap = findViewById<TextView>(R.id.goToMap)

        titleTv.text = placeName
        if (!photoUrl.isNullOrEmpty()) {
            Picasso.get().load(photoUrl).placeholder(R.drawable.logo_rostov).into(imageMain)
        }

        // Загружаем текущий рейтинг из БД по ID
        loadRating()

        // Слушатели для звезд
        for (i in 0 until starsContainer.childCount) {
            starsContainer.getChildAt(i).setOnClickListener {
                Log.d(TAG, "Star clicked: ${i + 1}")
                setRatingUI(i + 1)
            }
        }

        btnPublish.setOnClickListener { updateRatingInSupabase() }
        btnBack.setOnClickListener { finish() }
        btnAddPhoto.setOnClickListener { pickImage.launch("image/*") }

        goToMap.setOnClickListener {
            val intent = Intent(this, MainPage::class.java).apply {
                putExtra("open_map", true)
                putExtra("map_address", placeName)
            }
            startActivity(intent)
        }
    }

    private fun setRatingUI(rating: Int) {
        currentRating = rating
        val starsContainer = findViewById<LinearLayout>(R.id.linearLayout)
        for (i in 0 until starsContainer.childCount) {
            val star = starsContainer.getChildAt(i) as ImageView
            if (i < rating) {
                star.setBackgroundResource(R.drawable.blue_back)
            } else {
                star.setBackgroundResource(R.drawable.resource_bg_unselected)
            }
        }
    }

    private fun loadRating() {
        if (placeId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val place = withContext(Dispatchers.IO) {
                    supabase.from("places").select {
                        filter { eq("id", placeId) }
                    }.decodeSingleOrNull<ru.rostov.LiveParser.Place>()
                }

                if (place != null) {
                    Log.i(TAG, "Loaded rating: ${place.rating}")
                    setRatingUI(place.rating)
                } else {
                    Log.w(TAG, "No record found for ID: $placeId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rating: ${e.message}")
            }
        }
    }

    private fun updateRatingInSupabase() {
        if (placeId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID отсутствует", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val updateData = buildJsonObject {
                    put("id", placeId)
                    put("name", placeName)
                    put("rating", currentRating)
                }

                withContext(Dispatchers.IO) {
                    supabase.from("places").upsert(
                        updateData,
                        onConflict = "id"
                    )
                }

                Log.i(TAG, "Rating updated for ID: $placeId")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AboutAttraction, "Рейтинг опубликован", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AboutAttraction, "Ошибка публикации", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImageToSupabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "${placeName.filter { it.isLetterOrDigit() }}_${System.currentTimeMillis()}.png"
                val encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch

                val request = Request.Builder()
                    .url("$storageBaseUrl/$encodedName")
                    .post(bytes.toRequestBody("image/png".toMediaTypeOrNull()))
                    .addHeader("Authorization", "Bearer ${SupaBaseConfig.supabaseKey.supabaseKey}")
                    .addHeader("apikey", SupaBaseConfig.supabaseKey.supabaseKey)
                    .build()

                val response = OkHttpClient().newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Image uploaded: $fileName")
                        Toast.makeText(this@AboutAttraction, "Фото загружено", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Upload failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error: ${e.message}")
            }
        }
    }
}