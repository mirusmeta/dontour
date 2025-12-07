package ru.dontour

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class AdapterAttractions(var items: List<Attraction>) :
    RecyclerView.Adapter<AdapterAttractions.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.title_tv)
        val descText: TextView = view.findViewById(R.id.description_tv)
        val picture: ImageView = view.findViewById(R.id.picture)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attraction_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameText.text = item.name
        holder.descText.text = item.description

        loadImage(holder, item.pic)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AboutAttraction::class.java).apply {
                putExtra("id", item.id)
                putExtra("name", item.name)
                putExtra("desc", item.description)
                putExtra("pic", item.pic)
                putExtra("wiki", item.wiki_link)
            }
            context.startActivity(intent)
        }
    }

    private fun loadImage(holder: ViewHolder, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            holder.progressBar.visibility = View.VISIBLE

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.mipmap.ic_launcher_foreground)
                .error(R.mipmap.ic_launcher_foreground)
                .into(holder.picture, object : Callback {
                    override fun onSuccess() {
                        holder.progressBar.visibility = View.GONE
                    }

                    override fun onError(e: Exception?) {
                        holder.progressBar.visibility = View.GONE
                        holder.picture.setImageResource(R.mipmap.ic_launcher_foreground)
                    }
                })
        } else {
            holder.progressBar.visibility = View.GONE
            holder.picture.setImageResource(R.mipmap.ic_launcher_foreground)
        }
    }
}