package ru.rostov

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import ru.rostov.LiveParser.Place

class AttractionsAdapter(private var items: List<Place>) :
    RecyclerView.Adapter<AttractionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title_tv)
        val image: ImageView = view.findViewById(R.id.picture)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attraction_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name

        if (item.photos.isNotEmpty()) {
            Picasso.get().load(item.photos[0]).placeholder(R.drawable.logo_rostov).into(holder.image)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, AboutAttraction::class.java).apply {
                putExtra("place_name", item.name)
                putExtra("place_photo", if (item.photos.isNotEmpty()) item.photos[0] else "")
                putExtra("place_id", item.id)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Place>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun removeFirstItem() {
        if (items.isNotEmpty()) {
            items = items.drop(1)
            notifyItemRemoved(0)
        }
    }
}