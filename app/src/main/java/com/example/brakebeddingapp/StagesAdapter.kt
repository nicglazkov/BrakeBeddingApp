package com.example.brakebeddingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StagesAdapter(
    private val stages: List<BeddingStage>
) : RecyclerView.Adapter<StagesAdapter.StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        holder.bind(stage, position + 1)
    }

    override fun getItemCount(): Int = stages.size

    inner class StageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stageInfoTextView: TextView = itemView.findViewById(R.id.stageInfoTextView)

        fun bind(stage: BeddingStage, stageNumber: Int) {
            val brakingText = stage.brakingIntensity?.let { "Braking: ${it.displayName}" } ?: "Braking: Not specified"

            stageInfoTextView.text = """
                Stage $stageNumber
                Stops: ${stage.numberOfStops}
                Start Speed: ${stage.startSpeed} mph
                Target Speed: ${stage.targetSpeed} mph
                Gap Distance: ${stage.gapDistance} miles
                $brakingText
            """.trimIndent()
        }
    }
}