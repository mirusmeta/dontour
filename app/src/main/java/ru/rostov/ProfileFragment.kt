package ru.rostov

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.vk.id.VKID
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import ru.rostov.database.SupaBaseConfig

class ProfileFragment : Fragment() {

    private lateinit var ava: ImageView
    private lateinit var names: TextView
    private lateinit var logout: ConstraintLayout

    var name = ""
    var id = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        ava = view.findViewById(R.id.ava)
        names = view.findViewById(R.id.names)
        logout = view.findViewById(R.id.logout)
        val groupInvalid = view.findViewById<View>(R.id.group_invalid)

        groupInvalid.setOnClickListener {
            showInvalidGroupDialog()
        }

        loadInvalidGroup()
        loadVkAvatar()

        names.text = VKID.instance.accessToken!!.userData.firstName + " " + VKID.instance.accessToken!!.userData.lastName
        name = VKID.instance.accessToken!!.userData.firstName + " " + VKID.instance.accessToken!!.userData.lastName
        id = VKID.instance.accessToken!!.userID.toString()

        logout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                logoutVk()
            }
        }

        return view
    }

    private fun loadInvalidGroup() {

        val prefs = requireContext()
            .getSharedPreferences("data", Context.MODE_PRIVATE)

        val saved = prefs.getString("invalid_group", null)

        if (!saved.isNullOrEmpty()) {
            view?.findViewById<TextView>(R.id.levelText)?.setText(saved)
        }
    }
    private fun showInvalidGroupDialog() {
        val options = arrayOf("Нет", "1", "2", "3")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Группа инвалидности")
            .setItems(options) { _, which ->
                val selected = options[which]
                view?.findViewById<TextView>(R.id.levelText)?.text = selected

                requireContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
                    .putString("invalid_group", selected)
                    .apply()

                val userUpdate = User(
                    id = id,
                    name = name,
                    invalid = selected
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        android.util.Log.d("SupabaseLog", "Попытка отправки: $userUpdate")

                        val supabase = createSupabaseClient(
                            supabaseUrl = SupaBaseConfig.supabaseUrl.supabaseUrl,
                            supabaseKey = SupaBaseConfig.supabaseKey.supabaseKey
                        ) {
                            install(Postgrest)
                        }

                        supabase.from("users").upsert(userUpdate)

                        android.util.Log.d("SupabaseLog", "Данные успешно обновлены в БД")
                    } catch (e: Exception) {
                        android.util.Log.e("SupabaseLog", "Ошибка при отправке: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            .show()
    }

    private fun loadVkAvatar() {
        val originalUrl = VKID.instance.accessToken!!.userData.photo200
        val hdUrl = extractVkAvatarUrl(originalUrl)

        if (!hdUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(hdUrl)
                .fit()
                .centerCrop()
                .into(ava)
        }
    }

    private suspend fun logoutVk() {

        VKID.instance.logout(object : VKIDLogoutCallback {

            override fun onSuccess() {
                // твои SharedPreferences
                val prefs = requireContext()
                    .getSharedPreferences("data", android.content.Context.MODE_PRIVATE)

                prefs.edit()
                    .putBoolean("first_meet", false)
                    .apply()

                // переход
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }

            override fun onFail(fail: VKIDLogoutFail) {
                Toast.makeText(requireActivity(), "Не получилось(", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun extractVkAvatarUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrEmpty()) return null

        val base = originalUrl.substringBefore("?")
        val query = originalUrl.substringAfter("?", "")

        if (query.isEmpty()) return originalUrl

        val params = query.split("&").map { p ->
            if (p.startsWith("cs=")) "cs=400x400" else p
        }

        return "$base?${params.joinToString("&")}"
    }
}