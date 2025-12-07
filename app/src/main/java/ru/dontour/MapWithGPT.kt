package ru.dontour

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiRouteBuilder {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")
    }

    // --- Определяем файл по названию города ---
    private fun cityToFileName(city: String): String? = when (city) {
        "Ростов-на-Дону" -> "rostov.txt"
        "Rostov" -> "rostov.txt"
        "Таганрог" -> "tagonrog.txt"
        "Taganrog" -> "tagonrog.txt"
        "Азов" -> "azov.txt"
        "Azov" -> "azov.txt"
        "Новочеркасск" -> "novocherkassk.txt"
        "Novocherkassk" -> "novocherkassk.txt"
        "Волгодонск" -> "volgodonsk.txt"
        "Volgodonsk" -> "volgodonsk.txt"
        "Цимлянск" -> "cimlansk.txt"
        "Tsimlyansk" -> "cimlansk.txt"
        "Семикаракорск" -> "semikarakorsk.txt"
        "Semikarakorsk" -> "semikarakorsk.txt"
        "Батайск" -> "bataysk.txt"
        "Bataysk" -> "bataysk.txt"
        "Новошахтинск" -> "novoshahtinsk.txt"
        "Novoshakhtinsk" -> "novoshahtinsk.txt"
        "Старочеркасская" -> "starocherkassk.txt"
        "Starocherkasskaya" -> "starocherkassk.txt"
        "Шахты" -> "shahti.txt"
        "Shakhty" -> "shahti.txt"
        else -> null
    }

    // --- Безопасно читаем текст достопримечательностей ---
    private suspend fun loadFile(context: Context, fileName: String): String =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e("AI_FILE", "Ошибка при чтении $fileName из assets", e)
                ""
            }
        }

    /**
     * Строит маршрут по достопримечательностям указанного города.
     * @param context контекст Android
     * @param city название города (должен совпадать с кейсом в cityToFileName)
     * @param startLat широта пользователя
     * @param startLon долгота пользователя
     */
    suspend fun buildRoute(
        context: Context,
        city: String,
        startLat: Double,
        startLon: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = cityToFileName(city)
            if (fileName == null) {
                Log.e("AI_ROUTE_ERROR", "❌ Нет файла для города: $city")
                return@withContext null
            }

            val fileText = loadFile(context, fileName)
            if (fileText.isBlank()) {
                Log.e("AI_ROUTE_ERROR", "⚠️ Файл $fileName пустой")
                return@withContext null
            }

            // --- Формируем промпт под конкретный город ---
            val prompt = """
                Ты — нейросеть, которая строит туристический маршрут по достопримечательностям города $city.
                На вход тебе даются координаты пользователя и список всех достопримечательностей из базы.
                Твоя задача — выбрать 3–5 ближайших или интересных точек и вернуть их строго в формате:
                Название:широта,долгота|
                Без описаний, комментариев, переносов строк или лишних символов.
                
                Координаты пользователя: $startLat, $startLon
                
                База достопримечательностей:
                $fileText
            """.trimIndent()

            Log.d("AI_PROMPT", "Отправляем запрос для города $city")
            val response = model.generateContent(prompt)
            val text = response.text?.trim()?.replace("\n", "")

            if (text.isNullOrEmpty()) {
                Log.e("AI_ROUTE_ERROR", "Пустой ответ от модели для $city")
                return@withContext null
            }

            Log.d("AI_ROUTE", "✅ AI маршрут для $city: $text")
            text
        } catch (e: Exception) {
            Log.e("AI_ROUTE_ERROR", "Ошибка при построении маршрута", e)
            null
        }
    }
}