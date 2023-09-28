package ru.netology.yandexmaps.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ru.netology.yandexmaps.R
import ru.netology.yandexmaps.model.Model

class MyListAdapter(var mCtx: Context, var resource: Int, var items: List<Model>) :
    ArrayAdapter<Model>(mCtx, resource, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val LayoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        val view: View = LayoutInflater.inflate(resource, null)
        val textView: TextView = view.findViewById(R.id.idtext)
        val lat: TextView = view.findViewById(R.id.lat)
        val lon: TextView = view.findViewById(R.id.lon)
        var mItems: Model = items[position]
        textView.text = mItems.title
        lat.text = mItems.lat
        lon.text = mItems.lon
        return view
    }
}