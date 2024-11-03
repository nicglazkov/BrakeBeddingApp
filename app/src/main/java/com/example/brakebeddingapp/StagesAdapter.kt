package com.example.brakebeddingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StagesAdapter(
    private val stages: List<Stage>
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

        fun bind(stage: Stage, stageNumber: Int) {
            val text = when (stage) {
                is BeddingStage -> """
                    Stage $stageNumber (Bedding)
                    Stops: ${stage.numberOfStops}
                    Start Speed: ${stage.startSpeed} mph
                    Target Speed: ${stage.targetSpeed} mph
                    Gap Distance: ${stage.gapDistance} miles
                    Braking: ${stage.brakingIntensity.displayName}
                """.trimIndent()

                is CooldownStage -> """
                    Stage $stageNumber (Cooldown)
                    Distance: ${stage.distance} miles
                    Instructions: Drive without heavy braking to cool brakes
                """.trimIndent()
            }
            stageInfoTextView.text = text
        }
    }
}