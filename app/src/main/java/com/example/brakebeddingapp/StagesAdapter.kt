package com.example.brakebeddingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StagesAdapter(
    private val stages: List<BeddingStage>,
    private val onStageLongClick: (Int) -> Unit
) : RecyclerView.Adapter<StagesAdapter.StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        holder.bind(stage)

        // Set long-click listener to delete the item
        holder.itemView.setOnLongClickListener {
            onStageLongClick(position)
            true
        }
    }

    override fun getItemCount(): Int = stages.size

    inner class StageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stageInfoTextView: TextView = itemView.findViewById(R.id.stageInfoTextView)

        fun bind(stage: BeddingStage) {
            stageInfoTextView.text = "Stops: ${stage.numberOfStops}, Start: ${stage.startSpeed} mph, Target: ${stage.targetSpeed} mph, Gap: ${stage.gapDistance} miles"
        }
    }
}
