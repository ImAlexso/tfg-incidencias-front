package com.incidencias.ui.technician.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.R
import com.incidencias.databinding.FragmentTechnicianHomeBinding
import com.incidencias.ui.settings.SettingsActivity
import com.incidencias.ui.technician.incidents.MyAssignedIncidentsFragment
import com.incidencias.ui.technician.incidents.TeamUnassignedIncidentsFragment
import kotlinx.coroutines.launch

class TechnicianHomeFragment : Fragment(R.layout.fragment_technician_home) {

    private var _binding: FragmentTechnicianHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicianHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTechnicianHomeBinding.bind(view)

        setupClicks()
        observeState()
        viewModel.loadCounts()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadCounts()
        }
    }

    private fun setupClicks() {
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

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvTeamQueueCount.text = if (state.isLoading) "…" else state.teamQueueCount.toString()
                    binding.tvMyAssignedCount.text = if (state.isLoading) "…" else state.myAssignedCount.toString()

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
