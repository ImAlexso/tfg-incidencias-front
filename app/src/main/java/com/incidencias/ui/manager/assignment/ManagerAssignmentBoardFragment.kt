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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.incidencias.R
import com.incidencias.data.remote.dto.catalog.TeamTechnicianResponse
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.FragmentManagerAssignmentBoardBinding
import com.incidencias.ui.incident.IncidentDetailActivity
import com.incidencias.ui.manager.ManagerMainActivity
import kotlinx.coroutines.launch

class ManagerAssignmentBoardFragment : Fragment(R.layout.fragment_manager_assignment_board) {

    private var _binding: FragmentManagerAssignmentBoardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerAssignmentViewModel by viewModels()

    private lateinit var incidentAdapter: UnassignedIncidentDragAdapter
    private var currentTechnicians: List<TeamTechnicianResponse> = emptyList()

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
    }

    private fun setupRecycler() {
        incidentAdapter = UnassignedIncidentDragAdapter(
            items = emptyList(),
            onClick = { incident ->
                openIncidentDetail(incident)
            },
            onAssignClick = { incident ->
                showAssignDialog(incident)
            }
        )

        binding.recyclerUnassigned.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUnassigned.adapter = incidentAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBoard(forceRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadBoard()
        }
    }

    private fun openIncidentDetail(incident: IncidentListItemResponse) {
        val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
        intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
        startActivity(intent)
    }

    private fun showAssignDialog(incident: IncidentListItemResponse) {
        if (currentTechnicians.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay técnicos disponibles en este equipo",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val technicianNames = currentTechnicians.map { technician ->
            val name = technician.fullName.ifBlank { "Técnico ${technician.id}" }
            val email = technician.email.ifBlank { "Sin email" }
            "$name · $email"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${incident.referenceCode} · Selecciona técnico")
            .setItems(technicianNames) { _, which ->
                val technician = currentTechnicians[which]
                viewModel.assignIncident(
                    incident.id,
                    technician.id,
                    technician.fullName
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    currentTechnicians = state.technicians

                    binding.layoutLoading.visibility =
                        if (state.isLoading && state.unassignedIncidents.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.unassignedIncidents.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvError.text = state.errorMessage.orEmpty()

                    incidentAdapter.submitList(state.unassignedIncidents)

                    binding.tvSummary.text =
                        "${state.unassignedIncidents.size} incidencias sin asignar · ${state.technicians.size} técnicos disponibles"

                    binding.tvHelper.text =
                        if (state.technicians.isEmpty()) {
                            "No hay técnicos disponibles para asignar ahora mismo."
                        } else {
                            "Pulsa en Asignar para repartir una incidencia al técnico del equipo que corresponda."
                        }

                    binding.tvUnassignedEmpty.visibility =
                        if (state.unassignedIncidents.isEmpty() && state.errorMessage == null && !state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    if (state.errorMessage != null && state.unassignedIncidents.isNotEmpty()) {
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