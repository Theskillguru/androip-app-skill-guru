package com.example.theskillguru

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import androidx.core.content.ContextCompat

class DropdownAdapter(
    private val context: Context,
    private val options: Array<String>,
    private val selectedItems: MutableSet<String>
) : BaseAdapter() {

    private var filteredOptions: List<String> = options.drop(1).toList()

    override fun getCount(): Int = filteredOptions.size

    override fun getItem(position: Int): String = filteredOptions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_with_checkbox, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val option = filteredOptions[position]

        // Clear previous listener to avoid multiple triggers
        viewHolder.checkBox.setOnCheckedChangeListener(null)

        // Set the checkbox text and state
        viewHolder.checkBox.text = option
        viewHolder.checkBox.isChecked = selectedItems.contains(option)

        // Update the background immediately based on the checkbox state
        updateBackground(view, viewHolder.checkBox.isChecked)

        // Set listener for checkbox state changes
        viewHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(option)
            } else {
                selectedItems.remove(option)
            }

            // Update the background instantly when checkbox state changes
            updateBackground(view, isChecked)
        }

        return view
    }


    private fun updateBackground(view: View, isChecked: Boolean) {
        val backgroundColor = if (isChecked) {
            ContextCompat.getColor(context, R.color.blue) // Your selected background color
            ContextCompat.getColor(context,R.color.blue)
        } else {
            ContextCompat.getColor(context, R.color.white) // Your default background color
        }
        view.setBackgroundColor(backgroundColor)
    }


    fun filter(query: String) {
        filteredOptions = if (query.isEmpty()) {
            options.drop(1)
        } else {
            options.drop(1).filter { it.contains(query, ignoreCase = true) }
        }

        // Ensure the correct state of checkboxes after filtering
        notifyDataSetChanged()
    }



    private class ViewHolder(view: View) {
        val  checkBox :CheckBox = view.findViewById(R.id.checkbox)

    }
}