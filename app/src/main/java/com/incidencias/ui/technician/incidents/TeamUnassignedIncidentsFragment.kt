package com.incidencias.ui.technician.incidents

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.databinding.BottomSheetActiveFiltersBinding
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import com.incidencias.ui.technician.TechnicianMainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TeamUnassignedIncidentsFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicianIncidentsViewModel by viewModels()
    private lateinit var incidentAdapter: IncidentAdapter

    private var allIncidents: List<IncidentListItemResponse> = emptyList()
    private var selectedPriority: String? = null
    private var filteredCount: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        (requireActivity() as? TechnicianMainActivity)?.setToolbarTitle("Pendientes del equipo")

        setupRecycler()
        setupRefresh()
        setupFilters()
        observeUiState()

        if (savedInstanceState == null) {
            viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED, forceRefresh = true)
        }
    }

    private fun setupRecycler() {
        incidentAdapter = IncidentAdapter(
            showAssignedTechnician = true,
            showAssignToMeAction = true,
            onItemClick = { incident ->
                openIncidentDetail(incident.id)
            },
            onAssignToMeClick = { incident ->
                assignToMe(incident.id)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = incidentAdapter

        binding.btnRetry.setOnClickListener {
            viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED)
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED, forceRefresh = true)
        }
    }

    private fun setupFilters() {
        binding.btnOpenFilters.setOnClickListener {
            showFiltersBottomSheet()
        }
        updateFilterButtonText()
    }

    private fun showFiltersBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetActiveFiltersBinding.inflate(LayoutInflater.from(requireContext()))
        sheetBinding.rgStatus.visibility = View.GONE
        sheetBinding.tvStatusLabel.visibility = View.GONE
        dialog.setContentView(sheetBinding.root)

        when (selectedPriority) {
            null -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityAll.id)
            "LOW" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityLow.id)
            "MEDIUM" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityMedium.id)
            "HIGH" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityHigh.id)
            "CRITICAL" -> sheetBinding.rgPriority.check(sheetBinding.rbPriorityCritical.id)
        }

        sheetBinding.btnClearFilters.setOnClickListener {
            selectedPriority = null
            applyFilters()
            dialog.dismiss()
        }

        sheetBinding.btnApplyFilters.setOnClickListener {
            selectedPriority = when (sheetBinding.rgPriority.checkedRadioButtonId) {
                sheetBinding.rbPriorityLow.id -> "LOW"
                sheetBinding.rbPriorityMedium.id -> "MEDIUM"
                sheetBinding.rbPriorityHigh.id -> "HIGH"
                sheetBinding.rbPriorityCritical.id -> "CRITICAL"
                else -> null
            }

            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateFilterButtonText() {
        val hasFilters = selectedPriority != null

        binding.btnOpenFilters.text = if (hasFilters) {
            "Filtrar · $filteredCount resultados"
        } else {
            "Filtrar"
        }
    }

    private fun applyFilters() {
        val filtered = allIncidents.filter { incident ->
            selectedPriority == null ||
                    incident.priorityName.equals(selectedPriority, ignoreCase = true)
        }

        filteredCount = filtered.size
        updateFilterButtonText()

        incidentAdapter.submitList(filtered)

        binding.recyclerView.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
        binding.layoutEmpty.visibility =
            if (filtered.isEmpty() && !viewModel.uiState.value.isLoading && viewModel.uiState.value.errorMessage == null) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (filtered.isEmpty() && allIncidents.isNotEmpty()) {
            binding.tvEmpty.text = "No hay incidencias que coincidan con los filtros"
        } else if (allIncidents.isEmpty()) {
            binding.tvEmpty.text =
                viewModel.uiState.value.emptyMessage ?: "No hay incidencias pendientes de asignación en tu equipo"
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

                    allIncidents = state.incidents
                    applyFilters()

                    if (state.errorMessage != null && state.incidents.isNotEmpty()) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun openIncidentDetail(incidentId: Long) {
        val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
        intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incidentId)
        startActivity(intent)
    }

    private fun assignToMe(incidentId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SessionManager(requireContext()).userIdFlow.first()
                if (userId == null) {
                    Toast.makeText(requireContext(), "No se pudo identificar tu sesión.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val repository = IncidentRepository(requireContext())
                val response = repository.assignTechnician(incidentId, userId)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Incidencia asignada correctamente.", Toast.LENGTH_LONG).show()
                    viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED, forceRefresh = true)
                } else {
                    val message = when (response.code()) {
                        400 -> "La incidencia ya no se puede asignar."
                        403 -> "No tienes permiso para asignarte esta incidencia."
                        404 -> "La incidencia ya no existe."
                        else -> "No se pudo asignar la incidencia."
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.loadIncidents(TechnicianListMode.TEAM_UNASSIGNED, forceRefresh = true)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message ?: "No se pudo asignar la incidencia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
