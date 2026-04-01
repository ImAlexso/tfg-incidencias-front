package com.incidencias.ui.manager.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.R
import com.incidencias.databinding.FragmentManagerHomeBinding
import com.incidencias.ui.manager.ManagerMainActivity
import com.incidencias.ui.manager.incidents.ManagerIncidentsFragment
import com.incidencias.ui.manager.incidents.PendingClosureFragment
import com.incidencias.ui.manager.team.ManagerTeamFragment
import kotlinx.coroutines.launch
import com.incidencias.ui.notifications.NotificationsFragment

class ManagerHomeFragment : Fragment(R.layout.fragment_manager_home) {

    private var _binding: FragmentManagerHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManagerHomeBinding.bind(view)

        (requireActivity() as? ManagerMainActivity)?.setToolbarTitle("Portal manager")

        setupClicks()
        observeState()

        if (savedInstanceState == null) {
            viewModel.loadHomeData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadHomeData(forceRefresh = true)
        }
    }

    private fun setupClicks() {
        binding.cardTeamIncidents.setOnClickListener {
            openIncidents()
        }

        binding.cardTeamMembers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, ManagerTeamFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardUnassigned.setOnClickListener {
            openIncidents(onlyUnassigned = true)
        }

        binding.cardResolved.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, PendingClosureFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardCritical.setOnClickListener {
            openIncidents(priority = "CRITICAL")
        }
        binding.cardNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun openIncidents(
        status: String? = null,
        priority: String? = null,
        onlyUnassigned: Boolean = false
    ) {
        val fragment = ManagerIncidentsFragment().apply {
            arguments = bundleOf(
                ManagerIncidentsFragment.ARG_STATUS to status,
                ManagerIncidentsFragment.ARG_PRIORITY to priority,
                ManagerIncidentsFragment.ARG_ONLY_UNASSIGNED to onlyUnassigned
            )
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.managerFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val firstName = state.firstName?.takeIf { it.isNotBlank() } ?: "manager"

                    binding.tvWelcome.text = "Bienvenido, $firstName"
                    binding.tvSubtitle.text =
                        "Supervisa la carga del equipo y entra rápido a las incidencias que necesitan acción"

                    binding.tvUnassignedCount.text = state.unassignedCount.toString()
                    binding.tvResolvedCount.text = state.resolvedCount.toString()
                    binding.tvCriticalCount.text = state.criticalCount.toString()
                    binding.tvActiveIncidentsCount.text = state.activeIncidentsCount.toString()
                    binding.tvTeamMembersCount.text = state.teamMembersCount.toString()

                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
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