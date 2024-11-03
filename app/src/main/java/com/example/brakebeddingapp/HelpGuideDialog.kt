package com.example.brakebeddingapp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HelpGuideDialog : DialogFragment() {

    private data class HelpSection(
        val title: String,
        val content: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_help_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.helpSectionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val helpSections = listOf(
            HelpSection(
                "What is Brake Bedding?",
                """
                Brake bedding is the process of properly breaking in new brake pads and/or rotors. 
                This process deposits an even layer of brake pad material onto the rotor surface and 
                gradually heats the brakes to properly cure the pad material.
                
                A proper brake bedding procedure helps:
                • Prevent brake squeal
                • Improve brake performance
                • Extend brake life
                • Prevent brake fade
                """.trimIndent()
            ),
            HelpSection(
                "Setting Up Stages",
                """
                1. Click 'Settings' to configure your brake bedding stages
                2. For each stage, enter:
                   • Number of stops
                   • Starting speed
                   • Target speed (after braking)
                   • Distance between stops
                   • Braking intensity
                3. Add a cooldown stage at the end
                4. Click 'Save' when done
                
                Example routine:
                • Stage 1: 20 light stops from 42→18 mph, 0.3 miles between
                • Stage 2: 10 medium stops from 54→30 mph, 0.62 miles between
                • Stage 3: 10 hard stops from 72→30 mph, minimal distance
                • Cooldown: Drive 6 miles without heavy braking
                """.trimIndent()
            ),
            HelpSection(
                "Running the Procedure",
                """
                1. Find a safe, empty road
                2. Click 'Start' on the main screen
                3. Follow the on-screen instructions:
                   • GREEN: Accelerate to target speed
                   • BLUE: Hold the speed steady
                   • RED: Apply brakes as instructed
                   • GREEN: Drive cooldown distance
                4. Watch the speed indicator and color-coded instructions
                5. Complete all cycles in each stage
                """.trimIndent()
            ),
            HelpSection(
                "Safety Guidelines",
                """
                • Only perform brake bedding on empty, safe roads
                • Maintain awareness of your surroundings
                • Don't exceed posted speed limits
                • Watch your mirrors during braking
                • Stop if you smell burning or feel brake fade
                • Allow full cool-down before parking
                """.trimIndent()
            ),
            HelpSection(
                "Tips for Success",
                """
                • Choose a straight, level road
                • Plan your route to have enough distance
                • Don't drag the brakes between stops
                • Complete the full cooldown stage
                • Don't park immediately after completion
                """.trimIndent()
            )
        )

        recyclerView.adapter = HelpSectionsAdapter(helpSections)

        view.findViewById<View>(R.id.closeButton).setOnClickListener {
            dismiss()
        }
    }

    private class HelpSectionsAdapter(private val sections: List<HelpSection>) :
        RecyclerView.Adapter<HelpSectionsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleView: TextView = view.findViewById(R.id.sectionTitle)
            val contentView: TextView = view.findViewById(R.id.sectionContent)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_help_section, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val section = sections[position]
            holder.titleView.text = section.title
            holder.contentView.text = section.content
        }

        override fun getItemCount() = sections.size
    }
}