package com.incidencias.ui.manager.incidents

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.incidencias.R
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.BottomSheetManagerFiltersBinding
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import com.incidencias.ui.manager.ManagerMainActivity
import kotlinx.coroutines.launch

class ManagerIncidentsFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerIncidentsViewModel by viewModels()
    private lateinit var incidentAdapter: IncidentAdapter

    private var filteredItems: List<IncidentListItemResponse> = emptyList()

    companion object {
        const val ARG_TECHNICIAN_ID = "arg_technician_id"
        const val ARG_TECHNICIAN_NAME = "arg_technician_name"
        const val ARG_STATUS = "arg_status"
        const val ARG_PRIORITY = "arg_priority"
        const val ARG_ONLY_UNASSIGNED = "arg_only_unassigned"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        val initialTechnicianId = if (arguments?.containsKey(ARG_TECHNICIAN_ID) == true) {
            arguments?.getLong(ARG_TECHNICIAN_ID)
        } else {
            null
        }

        val initialTechnicianName = arguments?.getString(ARG_TECHNICIAN_NAME)
        val initialStatus = arguments?.getString(ARG_STATUS)
        val initialPriority = arguments?.getString(ARG_PRIORITY)
        val initialOnlyUnassigned = arguments?.getBoolean(ARG_ONLY_UNASSIGNED, false) == true

        if (
            initialTechnicianId != null ||
            initialStatus != null ||
            initialPriority != null ||
            initialOnlyUnassigned
        ) {
            viewModel.updateFilters(
                status = initialStatus,
                priority = initialPriority,
                technicianId = initialTechnicianId,
                technicianName = initialTechnicianName,
                onlyUnassigned = initialOnlyUnassigned
            )
        }

        (requireActivity() as? ManagerMainActivity)?.setToolbarTitle("Incidencias del equipo")

        setupRecycler()
        setupRefresh()
        setupFilters()
        observeUiState()

        if (savedInstanceState == null) {
            viewModel.loadActiveIncidents()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadActiveIncidents(forceRefresh = true)
        }
    }

    private fun setupRecycler() {
        incidentAdapter = IncidentAdapter(
            showAssignedTechnician = true,
            showAssignToMeAction = false,
            onItemClick = { incident ->
                val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
                intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
                startActivity(intent)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = incidentAdapter

        binding.btnRetry.setOnClickListener {
            viewModel.loadActiveIncidents()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadActiveIncidents(forceRefresh = true)
        }
    }

    private fun setupFilters() {
        binding.btnOpenFilters.setOnClickListener {
            showFiltersBottomSheet()
        }
    }

    private fun showFiltersBottomSheet() {
        val state = viewModel.uiState.value
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetManagerFiltersBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(sheetBinding.root)

        when (state.selectedStatus) {
            null -> sheetBinding.rgStatus.check(sheetBinding.rbStatusAll.id)
            "OPEN" -> sheetBinding.rgStatus.check(sheetBinding.rbStatusOpen.id)
            "IN_PROGRESS" -> sheetBinding.rgStatus.check(sheetBinding.rbStatusInProgress.id)
            else -> sheetBinding.rgStatus.check(sheetBinding.rbStatusAll.id)
        }

        when (state.selectedPriority) {
            null -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityAll.id)
            "LOW" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityLow.id)
            "MEDIUM" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityMedium.id)
            "HIGH" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityHigh.id)
            "CRITICAL" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityCritical.id)
        }

        sheetBinding.switchOnlyUnassigned.isChecked = state.onlyUnassigned

        val technicianItems = listOf("Todos los técnicos") + state.technicians.map { it.fullName }
        val technicianAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            technicianItems
        )
        sheetBinding.actvTechnician.setAdapter(technicianAdapter)
        sheetBinding.actvTechnician.setText(state.selectedTechnicianName ?: "Todos los técnicos", false)

        sheetBinding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
            renderFilteredList()
            dialog.dismiss()
        }

        sheetBinding.btnApplyFilters.setOnClickListener {
            val selectedStatus = when (sheetBinding.rgStatus.checkedRadioButtonId) {
                sheetBinding.rbStatusOpen.id -> "OPEN"
                sheetBinding.rbStatusInProgress.id -> "IN_PROGRESS"
                else -> null
            }

            val selectedPriority = when (sheetBinding.rgPriority.checkedRadioButtonId) {
                sheetBinding.rbPriorityLow.id -> "LOW"
                sheetBinding.rbPriorityMedium.id -> "MEDIUM"
                sheetBinding.rbPriorityHigh.id -> "HIGH"
                sheetBinding.rbPriorityCritical.id -> "CRITICAL"
                else -> null
            }

            val selectedTechnicianName = sheetBinding.actvTechnician.text?.toString()
                ?.takeIf { it.isNotBlank() && it != "Todos los técnicos" }

            val selectedTechnician = state.technicians.firstOrNull { it.fullName == selectedTechnicianName }

            viewModel.updateFilters(
                status = selectedStatus,
                priority = selectedPriority,
                technicianId = selectedTechnician?.id,
                technicianName = selectedTechnician?.fullName,
                onlyUnassigned = sheetBinding.switchOnlyUnassigned.isChecked
            )

            renderFilteredList()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateFilterButtonText() {
        val state = viewModel.uiState.value
        val hasFilters =
            state.selectedStatus != null ||
                    state.selectedPriority != null ||
                    state.selectedTechnicianId != null ||
                    state.onlyUnassigned

        binding.btnOpenFilters.text = if (hasFilters) {
            "Filtrar · ${filteredItems.size} resultados"
        } else {
            "Filtrar"
        }
    }

    private fun renderFilteredList() {
        filteredItems = viewModel.getFilteredIncidents()
        incidentAdapter.submitList(filteredItems)
        updateFilterButtonText()

        binding.recyclerView.visibility = if (filteredItems.isNotEmpty()) View.VISIBLE else View.GONE
        binding.layoutEmpty.visibility =
            if (filteredItems.isEmpty() &&
                !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.errorMessage == null
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvEmpty.text = if (
            filteredItems.isEmpty() && viewModel.uiState.value.incidents.isNotEmpty()
        ) {
            "No hay incidencias que coincidan con los filtros"
        } else {
            viewModel.uiState.value.emptyMessage ?: "No hay incidencias activas del equipo"
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading && state.incidents.isEmpty()) View.VISIBLE else View.GONE

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.incidents.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvError.text = state.errorMessage.orEmpty()

                    renderFilteredList()

                    if (state.errorMessage != null && state.incidents.isNotEmpty()) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
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