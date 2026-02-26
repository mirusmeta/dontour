package ru.rostov.citymodule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.rostov.R

class CityAdapter(
    private val items: List<CityItem>,
    private val onItemClick: (CityItem) -> Unit
) : RecyclerView.Adapter<CityAdapter.CustomViewHolder>() {

    class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.itemTitle)
        val image: ImageView = itemView.findViewById(R.id.itemImage)
        val root: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.city_item, parent, false)
        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.title.text = item.title
        holder.image.setImageResource(item.imageResId)

        holder.root.setBackgroundResource(
            if (item.isSelected) R.drawable.resource_bg_selected
            else R.drawable.resource_bg
        )

        holder.itemView.setOnClickListener {
            if (!item.isSelected) {

                val previousIndex = items.indexOfFirst { it.isSelected }

                items.forEach { it.isSelected = false }
                item.isSelected = true

                if (previousIndex != -1) notifyItemChanged(previousIndex)
                notifyItemChanged(position)

                val anim = AnimationUtils.loadAnimation(context, R.anim.select_animation)
                holder.root.startAnimation(anim)

                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}