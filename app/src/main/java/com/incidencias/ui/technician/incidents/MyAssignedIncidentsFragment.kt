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
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import kotlinx.coroutines.launch

class MyAssignedIncidentsFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicianIncidentsViewModel by viewModels()
    private lateinit var incidentAdapter: IncidentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        setupRecycler()
        setupRefresh()
        observeUiState()


        if (savedInstanceState == null) {
            viewModel.loadIncidents(TechnicianListMode.MY_ASSIGNED)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadIncidents(TechnicianListMode.MY_ASSIGNED, forceRefresh = true)
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
            viewModel.loadIncidents(TechnicianListMode.MY_ASSIGNED)
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadIncidents(TechnicianListMode.MY_ASSIGNED, forceRefresh = true)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
