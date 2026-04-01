package com.incidencias.ui.manager.assignment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.databinding.FragmentManagerAssignmentBoardBinding
import com.incidencias.ui.incident.IncidentDetailActivity
import com.incidencias.ui.manager.ManagerMainActivity
import kotlinx.coroutines.launch

class ManagerAssignmentBoardFragment : Fragment(R.layout.fragment_manager_assignment_board) {

    private var _binding: FragmentManagerAssignmentBoardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerAssignmentViewModel by viewModels()

    private lateinit var incidentAdapter: UnassignedIncidentDragAdapter
    private lateinit var technicianAdapter: TechnicianDropAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManagerAssignmentBoardBinding.bind(view)

        (requireActivity() as? ManagerMainActivity)?.setToolbarTitle("Reparto visual")

        setupRecycler()
        observeState()
        observeEvents()

        if (savedInstanceState == null) {
            viewModel.loadBoard()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadBoard(forceRefresh = true)
        }
    }

    private fun setupRecycler() {
        incidentAdapter = UnassignedIncidentDragAdapter(emptyList()) { incident ->
            val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
            intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
            startActivity(intent)
        }

        technicianAdapter = TechnicianDropAdapter(emptyList()) { incidentId, technician ->
            viewModel.assignIncident(incidentId, technician.id, technician.fullName)
        }

        binding.recyclerUnassigned.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUnassigned.adapter = incidentAdapter

        binding.recyclerTechnicians.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTechnicians.adapter = technicianAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBoard(forceRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadBoard()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading && state.unassignedIncidents.isEmpty() && state.technicians.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.unassignedIncidents.isEmpty() && state.technicians.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvError.text = state.errorMessage.orEmpty()

                    incidentAdapter.submitList(state.unassignedIncidents)
                    technicianAdapter.submitList(state.technicians)

                    binding.tvUnassignedEmpty.visibility =
                        if (state.unassignedIncidents.isEmpty() && state.errorMessage == null && !state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvTechniciansEmpty.visibility =
                        if (state.technicians.isEmpty() && state.errorMessage == null && !state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    if (state.errorMessage != null && (state.unassignedIncidents.isNotEmpty() || state.technicians.isNotEmpty())) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ManagerAssignmentEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                        }
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