package com.incidencias.ui.manager.team

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.databinding.FragmentManagerTeamBinding
import com.incidencias.ui.manager.ManagerMainActivity
import com.incidencias.ui.manager.incidents.ManagerIncidentsFragment
import kotlinx.coroutines.launch

class ManagerTeamFragment : Fragment(R.layout.fragment_manager_team) {

    private var _binding: FragmentManagerTeamBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerTeamViewModel by viewModels()
    private lateinit var adapter: ManagerTeamAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManagerTeamBinding.bind(view)

        (requireActivity() as? ManagerMainActivity)?.setToolbarTitle("Técnicos del equipo")

        setupRecycler()
        setupRefresh()
        observeUiState()

        if (savedInstanceState == null) {
            viewModel.loadTeam()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupRecycler() {
        adapter = ManagerTeamAdapter { technician ->
            val fragment = ManagerIncidentsFragment().apply {
                arguments = bundleOf(
                    ManagerIncidentsFragment.ARG_TECHNICIAN_ID to technician.technicianId,
                    ManagerIncidentsFragment.ARG_TECHNICIAN_NAME to technician.technicianName
                )
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnRetry.setOnClickListener {
            viewModel.loadTeam()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTeam(forceRefresh = true)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.layoutLoading.visibility =
                        if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.items.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvError.text = state.errorMessage.orEmpty()

                    adapter.submitList(state.items)

                    binding.recyclerView.visibility =
                        if (state.items.isNotEmpty()) View.VISIBLE else View.GONE

                    val showEmpty =
                        state.items.isEmpty() && !state.isLoading && state.errorMessage == null

                    binding.emptyState.layoutEmpty.visibility =
                        if (showEmpty) View.VISIBLE else View.GONE

                    if (showEmpty) {
                        binding.emptyState.ivEmpty.setImageResource(R.drawable.ic_home_team)

                        binding.emptyState.tvEmpty.text =
                            state.emptyMessage ?: "No hay técnicos disponibles en el equipo"

                        binding.emptyState.tvEmptySubtitle.text =
                            "Cuando haya técnicos asociados al equipo, aparecerán aquí"

                        binding.emptyState.tvEmptySubtitle.visibility = View.VISIBLE
                    }

                    if (state.errorMessage != null && state.items.isNotEmpty()) {
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