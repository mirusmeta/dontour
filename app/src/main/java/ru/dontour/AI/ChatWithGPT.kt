package ru.dontour.AI

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dontour.AI.ChatMessage
import ru.dontour.AIPro
import ru.dontour.HomeFragment
import ru.dontour.MainPage
import ru.dontour.R
import java.util.Locale

class ChatWithGPT : AppCompatActivity() {

    private lateinit var adapter: AdapterAiChat
    private val messages = mutableListOf<ChatMessage>()

    object AiHelper {
        private lateinit var prefs: SharedPreferences
        private const val PREFS_NAME = "ai_memory"
        private const val KEY_HISTORY = "chat_history"

        private val chatHistory = mutableListOf<String>()

        private val model by lazy {
            Firebase.ai(
                backend = GenerativeBackend.Companion.googleAI()
            ).generativeModel("gemini-2.0-flash")
        }

        private const val systemPrompt = """
        Ты — дружелюбный помощник по путешествиям по Ростовской области.
        Отвечай как местный гид: рассказывай о местах, маршрутах.
        Не используй никакое форматирование (никаких **жирных**, *курсивных* или других Markdown элементов).
        Пиши естественно, коротко и по делу. 
        Если пользователь задаёт вопрос не по теме Ростовской области — всё равно попробуй связать ответ с этим регионом.
        Выдавай только правильный ответ, а чтобы было записано это запомни, это клеше, желательно на предложений 3-4.
    """

        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val saved = prefs.getString(KEY_HISTORY, null)
            if (!saved.isNullOrBlank()) {
                chatHistory.clear()
                chatHistory.addAll(saved.split("\n"))
            }
        }

        private fun saveHistory() {
            prefs.edit().putString(KEY_HISTORY, chatHistory.joinToString("\n")).apply()
        }

        suspend fun generate(context: Context, userPrompt: String): String? = withContext(Dispatchers.IO) {
            return@withContext try {
                chatHistory.add("Пользователь: $userPrompt")
                val historyContext = chatHistory.joinToString("\n")
                val language = context.getString(R.string.languagetoanswer)

                val fullPrompt = """
            $systemPrompt
            
            Язык, на котором ты будешь отвечать пользователю:
            $language
            
            История диалога:
            $historyContext

            Помощник:
        """.trimIndent()

                val response = model.generateContent(fullPrompt)
                val text = response.text?.replace(Regex("[*_`#>]"), "")?.trim()
                if (!text.isNullOrBlank()) {
                    chatHistory.add("Помощник: $text")
                }
                Log.d("AI_HELPER", "AI Выводит: $text")
                text
            } catch (e: Exception) {
                Log.e("AI_HELPER", "Ошибка при получении AI запроса", e)
                null
            }
        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_with_gpt)

        setupEdgeToEdge()
//        AiHelper.init(this)
        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@ChatWithGPT, MainPage::class.java)
            startActivity(intent)
            finish()
        }
        val buyAI = findViewById<ConstraintLayout>(R.id.buyAI)
        val sendButton = findViewById<ConstraintLayout>(R.id.send_button)
        val editText = findViewById<EditText>(R.id.edittext)
        val b_back = findViewById<ConstraintLayout>(R.id.b_back)

        val recycler = findViewById<RecyclerView>(R.id.chat_recycler)
        adapter = AdapterAiChat(messages)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        b_back.setOnClickListener {
            val intent = Intent(this@ChatWithGPT, MainPage::class.java)
            startActivity(intent)
        }

        sendButton.setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isEmpty()) {
                startVoiceRecognition()
            } else {
                if (text.isNotEmpty()) {
                    adapter.addMessage(ChatMessage(text, true))
                    recycler.scrollToPosition(messages.size - 1)
                    editText.text.clear()

                    makeResponse(text, recycler)
                }
            }
        }

        buyAI.setOnClickListener {
//            startActivity(Intent(this, AIPro::class.java))
            Toast.makeText(this, "Пока недоступно)", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeResponse(prompt: String, recycler: RecyclerView) {
        lifecycleScope.launch {
            val result = AiHelper.generate(this@ChatWithGPT, prompt)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    adapter.addMessage(ChatMessage(result, false))
                    recycler.scrollToPosition(messages.size - 1)
                } else {
                    Log.e("CHAT", "Ошибка: AI не вернул ответ")
                }
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи что-нибудь")
        }
        try {
            startActivityForResult(intent, 1001)
        } catch (_: Exception) { }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val editText = findViewById<EditText>(R.id.edittext)
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let {
                editText.setText(it)
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
    }

    override fun onBackPressed() {
        val intent = Intent(this@ChatWithGPT, HomeFragment::class.java)
        startActivity(intent)
    }

}