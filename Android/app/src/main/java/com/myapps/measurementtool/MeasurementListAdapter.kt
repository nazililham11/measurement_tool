package com.myapps.measurementtool

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
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

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val measurement = list[position]
        val date: Calendar = Calendar.getInstance()
        date.time = measurement.date
        holder.tvTitle.text  = measurement.title
        holder.tvLength.text = measurement.length.roundToInt().toString()
        holder.tvWidth.text  = measurement.width.roundToInt().toString()
        holder.tvHeight.text = measurement.height.roundToInt().toString()
        holder.tvDate.text   = SimpleDateFormat("dd").format(measurement.date) + " " + toIndonesianShortMonth(date.get(Calendar.MONTH))
        holder.tvTime.text   = SimpleDateFormat("HH:mm").format(measurement.date)
        holder.tvDay.text    = toIndonesianDays(date.get(Calendar.DAY_OF_WEEK))

    }

    private fun toIndonesianDays(dayOfWeek: Int): String {
        when (dayOfWeek) {
            1 -> return "Senin"
            2 -> return "Selasa"
            3 -> return "Rabu"
            4 -> return "Kamis"
            5 -> return "Jumat"
            6 -> return "Sabtu"
            7 -> return "Minggu"
        }
        return ""
    }

    private fun toIndonesianShortMonth(month: Int): String {
        when (month) {
            1 -> return "Jan"
            2 -> return "Feb"
            3 -> return "Mar"
            4 -> return "Apr"
            5 -> return "Mei"
            6 -> return "Jun"
            7 -> return "Jul"
            8 -> return "Agu"
            9 -> return "Sep"
            10 -> return "Okt"
            11 -> return "Nov"
            12 -> return "Des"
        }
        return ""
    }



    fun update(list:ArrayList<Measurement>){
        this.list = list
        this.notifyDataSetChanged()
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTitle:    TextView = itemView.findViewById(R.id.tv_title)
        var tvLength:   TextView = itemView.findViewById(R.id.tv_length_value)
        var tvWidth:    TextView = itemView.findViewById(R.id.tv_width_value)
        var tvHeight:   TextView = itemView.findViewById(R.id.tv_height_value)
        var tvDate:     TextView = itemView.findViewById(R.id.tv_date)
        var tvTime:     TextView = itemView.findViewById(R.id.tv_time)
        var tvDay:      TextView = itemView.findViewById(R.id.tv_day)

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(list[adapterPosition])
            }
        }
    }
}