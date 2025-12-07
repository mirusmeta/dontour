package ru.dontour

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

enum class ActionType {
    MAP, NACTIVITY, GOTOANOTHERAPP, NONE
}

data class TypeItem(
    val iconResId: Int,
    val title: String,
    val description: String,
    val actionType: ActionType = ActionType.NONE,
    val actionData: String? = null
)

class TypeAdapter(
    private var items: List<TypeItem>,
    private val context: Context
) : RecyclerView.Adapter<TypeAdapter.CustomViewHolder>() {

    companion object {
        private const val TYPE_ONE = 0
        private const val TYPE_TWO = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (position % 2 == 0) TYPE_ONE else TYPE_TWO

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.serviceIcon)
        val title: TextView = view.findViewById(R.id.serviceTitle)
        val description: TextView = view.findViewById(R.id.serviceDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutId = when (viewType) {
            TYPE_ONE -> R.layout.type_item_1
            TYPE_TWO -> R.layout.type_item_2
            else -> R.layout.type_item_1
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = items[position]

        holder.icon.setImageResource(item.iconResId)
        holder.title.text = item.title
        holder.description.text = item.description

        val anim = AnimationUtils.loadAnimation(context, R.anim.item_fade_in)
        holder.itemView.startAnimation(anim)

        holder.itemView.setOnClickListener {
            handleAction(item)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<TypeItem>) {
        items = newList
        notifyDataSetChanged()
    }

    // 🔹 Обработка кликов по типу действия
    @SuppressLint("UseKtx")
    private fun handleAction(item: TypeItem) {
        when (item.actionType) {
            ActionType.MAP -> {
                val geoUri = Uri.parse(item.actionData ?: "geo:0,0?q=Your+Place")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                context.startActivity(mapIntent)
            }

            ActionType.NACTIVITY -> {
                try {
                    val clazz = Class.forName(item.actionData ?: return)
                    val intent = Intent(context, clazz)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ActionType.GOTOANOTHERAPP -> {
                val uri = Uri.parse(item.actionData ?: return)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }

            ActionType.NONE -> {
                context.apply {
                    val intent = Intent(context, HomeFragment::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}
