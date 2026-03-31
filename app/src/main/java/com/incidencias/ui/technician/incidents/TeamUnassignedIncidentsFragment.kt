package com.incidencias.ui.technician.incidents

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
import com.incidencias.data.repository.IncidentRepository
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        (requireActivity() as? TechnicianMainActivity)?.setToolbarTitle("Pendientes del equipo")

        setupRecycler()
        setupRefresh()
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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = state.isRefreshing
                    binding.recyclerView.visibility = if (state.incidents.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.layoutEmpty.visibility =
                        if (state.incidents.isEmpty() && state.emptyMessage != null && !state.isLoading) View.VISIBLE else View.GONE
                    binding.layoutError.visibility =
                        if (state.errorMessage != null && state.incidents.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE

                    binding.tvEmpty.text = state.emptyMessage.orEmpty()
                    binding.tvError.text = state.errorMessage.orEmpty()

                    incidentAdapter.submitList(state.incidents)

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