package com.myapps.measurementtool

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.roundToInt

// Class adapter untuk listview
class MeasurementListAdapter(private var list: ArrayList<Measurement>)
    : RecyclerView.Adapter<MeasurementListAdapter.ListViewHolder>()
{


    var onItemClick: ((Measurement) -> Unit)? = null


    // Event saat membuat view holder 
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ListViewHolder
    {
        // Memberikan kerangka layout untuk item pada list
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_row_measurement, parent, false)
        return ListViewHolder(view)
    }

    // Fungsi untuk mengambil banyaknya item pada list
    override fun getItemCount() = list.size


    // Event saat menghubungka item pada list dengan viewholder  
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        // Mengambil data pada variable sesuai dengan posisi index
        val measurement = list[position]
        
        // Mengambil nilai waktu saat ini  
        val date: Calendar = Calendar.getInstance()
        date.time = measurement.date

        // Memngisi text pada element
        holder.tvTitle.text  = measurement.title
        holder.tvLength.text = measurement.length.roundToInt().toString()
        holder.tvWidth.text  = measurement.width.roundToInt().toString()
        holder.tvHeight.text = measurement.height.roundToInt().toString()

        // Memberikan thumbnail
        Utils.getBitmapFromPath(measurement.photo)?.let { image ->
            // Tampilkan gambar dengan ukuran 200px
            holder.ivThumbnails.setImageBitmap(Utils.resizeBitmap(image, 200))
            
            // Hilangkan text Image Not Found
            holder.tvImgNotFound.visibility = View.GONE
        
        // Apabila tidak terdapat thumbnail
        } ?: run {
            holder.ivThumbnails.setImageBitmap(null)
            // Tampilkan text Image Not Found
            holder.tvImgNotFound.visibility = View.VISIBLE
        }

    }

    // Fungsu untuk mengupdate ui apabila terdapat perubahan pada list
    fun update(list:ArrayList<Measurement>){
        this.list = list
        this.notifyDataSetChanged()
    }

    // Class untuk menghubungkan tiap list item dengan item dalam ui 
    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Menghubungkan variable dengan elemen ui
        var tvTitle:       TextView = itemView.findViewById(R.id.tv_title)
        var tvLength:      TextView = itemView.findViewById(R.id.tv_length_value)
        var tvWidth:       TextView = itemView.findViewById(R.id.tv_width_value)
        var tvHeight:      TextView = itemView.findViewById(R.id.tv_height_value)
        var tvImgNotFound: TextView = itemView.findViewById(R.id.tv_image_not_found)
        var ivThumbnails:  ImageView = itemView.findViewById(R.id.iv_thumbnail)

        // Inisialisasi 
        init {
            // Memeberikan event saat item ditekan 
            itemView.setOnClickListener {
                onItemClick?.invoke(list[adapterPosition])
            }
        }
    }
}