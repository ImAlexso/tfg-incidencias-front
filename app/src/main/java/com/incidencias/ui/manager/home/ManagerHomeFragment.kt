package com.incidencias.ui.manager.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.incidencias.R
import com.incidencias.databinding.FragmentManagerHomeBinding
import com.incidencias.ui.manager.incidents.ManagerIncidentsFragment
import com.incidencias.ui.manager.incidents.PendingClosureFragment
import com.incidencias.ui.settings.SettingsActivity

class ManagerHomeFragment : Fragment(R.layout.fragment_manager_home) {

    private var _binding: FragmentManagerHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManagerHomeBinding.bind(view)

        binding.cardTeamIncidents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, ManagerIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardPendingClosure.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, PendingClosureFragment())
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