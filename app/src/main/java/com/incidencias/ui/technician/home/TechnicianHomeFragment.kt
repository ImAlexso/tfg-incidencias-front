package com.incidencias.ui.technician.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.incidencias.R
import com.incidencias.databinding.FragmentTechnicianHomeBinding
import com.incidencias.ui.settings.SettingsActivity
import com.incidencias.ui.technician.incidents.MyAssignedIncidentsFragment
import com.incidencias.ui.technician.incidents.TeamUnassignedIncidentsFragment

class TechnicianHomeFragment : Fragment(R.layout.fragment_technician_home) {

    private var _binding: FragmentTechnicianHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTechnicianHomeBinding.bind(view)

        binding.cardTeamQueue.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, TeamUnassignedIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardMyAssigned.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, MyAssignedIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}