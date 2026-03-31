package com.incidencias.ui.user.incidents

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
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import kotlinx.coroutines.launch

class IncidentHistoryFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentsListViewModel by viewModels()
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        setupRecycler()
        setupRefresh()
        observeUiState()

        binding.tvScreenTitle.text = IncidentListMode.HISTORY.screenTitle
        binding.tvScreenSubtitle.text = IncidentListMode.HISTORY.screenSubtitle

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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.recyclerView.visibility =
                        if (state.incidents.isNotEmpty()) View.VISIBLE else View.GONE

                    binding.layoutEmpty.visibility =
                        if (state.incidents.isEmpty() && state.emptyMessage != null && !state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.incidents.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvEmpty.text = state.emptyMessage ?: ""
                    binding.tvError.text = state.errorMessage ?: ""

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
