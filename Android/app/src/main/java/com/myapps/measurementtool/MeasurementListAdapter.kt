package com.myapps.measurementtool

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.roundToInt

class MeasurementListAdapter(private var list: ArrayList<Measurement>)
    : RecyclerView.Adapter<MeasurementListAdapter.ListViewHolder>()
{

    var onItemClick: ((Measurement) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ListViewHolder
    {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_row_measurement, parent, false)
        return ListViewHolder(view)
    }


    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val measurement = list[position]
        val date: Calendar = Calendar.getInstance()
        date.time = measurement.date
        holder.tvTitle.text  = measurement.title
        holder.tvLength.text = measurement.length.roundToInt().toString()
        holder.tvWidth.text  = measurement.width.roundToInt().toString()
        holder.tvHeight.text = measurement.height.roundToInt().toString()
        Utils.getBitmapFromPath(measurement.photo)?.let { image ->
            holder.ivThumbnails.setImageBitmap(Utils.resizeBitmap(image, 200))
            holder.tvImgNotFound.visibility = View.GONE
        } ?: run {
            holder.ivThumbnails.setImageBitmap(null)
            holder.tvImgNotFound.visibility = View.VISIBLE
        }

    }

    fun update(list:ArrayList<Measurement>){
        this.list = list
        this.notifyDataSetChanged()
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTitle:       TextView = itemView.findViewById(R.id.tv_title)
        var tvLength:      TextView = itemView.findViewById(R.id.tv_length_value)
        var tvWidth:       TextView = itemView.findViewById(R.id.tv_width_value)
        var tvHeight:      TextView = itemView.findViewById(R.id.tv_height_value)
        var tvImgNotFound: TextView = itemView.findViewById(R.id.tv_image_not_found)
        var ivThumbnails:  ImageView = itemView.findViewById(R.id.iv_thumbnail)

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(list[adapterPosition])
            }
        }
    }
}