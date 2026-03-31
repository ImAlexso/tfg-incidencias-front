package com.incidencias.ui.user.incidents

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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.incidencias.R
import com.incidencias.data.remote.dto.incident.IncidentListItemResponse
import com.incidencias.databinding.BottomSheetHistoryFiltersBinding
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import com.incidencias.ui.user.UserMainActivity
import kotlinx.coroutines.launch

class IncidentHistoryFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentsListViewModel by viewModels()
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var allIncidents: List<IncidentListItemResponse> = emptyList()
    private var selectedPriority: String? = null
    private var filteredCount: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        (requireActivity() as? UserMainActivity)?.setToolbarTitle("Histórico")

        setupRecycler()
        setupRefresh()
        setupFilters()
        observeUiState()

        if (savedInstanceState == null) {
            viewModel.loadIncidents(IncidentListMode.HISTORY)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadIncidents(IncidentListMode.HISTORY, forceRefresh = true)
        }
    }

    private fun setupRecycler() {
        incidentAdapter = IncidentAdapter(onItemClick = { incident ->
            val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
            intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
            startActivity(intent)
        })

        layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = incidentAdapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalItems = layoutManager.itemCount

                if (lastVisible >= totalItems - 4) {
                    viewModel.loadMoreIncidents()
                }
            }
        })

        binding.btnRetry.setOnClickListener {
            viewModel.loadIncidents(IncidentListMode.HISTORY)
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadIncidents(IncidentListMode.HISTORY, forceRefresh = true)
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
        val sheetBinding = BottomSheetHistoryFiltersBinding.inflate(LayoutInflater.from(requireContext()))
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
        binding.btnOpenFilters.text = if (selectedPriority != null) {
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
            binding.tvEmpty.text = "No hay incidencias cerradas que coincidan con el filtro"
        } else if (allIncidents.isEmpty()) {
            binding.tvEmpty.text = viewModel.uiState.value.emptyMessage ?: "No hay incidencias cerradas"
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

                    binding.tvError.text = state.errorMessage ?: ""

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}