package com.example.sbmaproject.classes

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sbmaproject.R
import kotlinx.android.synthetic.main.exercise_result_item.view.*
import kotlin.math.round

class ExerciseRecyclerViewAdapter(
    private val exercises: MutableList<Exercise>?
) : RecyclerView.Adapter<ExerciseRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseRecyclerViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.exercise_result_item, parent, false)

        return ExerciseRecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseRecyclerViewHolder, position: Int) {
        val exercise = exercises?.get(position)

        if (exercise != null) {
            holder.itemView.displayName.text = exercise.username
            holder.itemView.timeTextView.text = exercise.time
            holder.itemView.maxSpeed.text = round(exercise.highestSpeed).toString() + " km/h"
            holder.itemView.averageSpeed.text = round(exercise.averageSpeed).toString() + " km/h"
            holder.itemView.steps.text = exercise.steps
            holder.itemView.dateText.text = exercise.date.toString().replaceAfter("GMT", "").replace("GMT", "")
            holder.itemView.distanceText.text = exercise.distance
        }
    }

    override fun getItemCount(): Int {
        return exercises?.size ?: 0
    }
}

class ExerciseRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view)