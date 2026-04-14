package com.incidencias.ui.technician.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.R
import com.incidencias.databinding.FragmentTechnicianHomeBinding
import com.incidencias.ui.notifications.NotificationsFragment
import com.incidencias.ui.technician.TechnicianMainActivity
import com.incidencias.ui.technician.incidents.MyAssignedIncidentsFragment
import com.incidencias.ui.technician.incidents.TeamUnassignedIncidentsFragment
import kotlinx.coroutines.launch

class TechnicianHomeFragment : Fragment(R.layout.fragment_technician_home) {

    private var _binding: FragmentTechnicianHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicianHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTechnicianHomeBinding.bind(view)

        (requireActivity() as? TechnicianMainActivity)?.setToolbarTitle("Portal técnico")

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
        binding.cardMyAssigned.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, MyAssignedIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardTeamQueue.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, TeamUnassignedIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val firstName = state.firstName?.takeIf { it.isNotBlank() } ?: "técnico"

                    binding.tvWelcome.text = "Bienvenido, $firstName"
                    binding.tvSubtitle.text =
                        "Consulta tus incidencias asignadas, la cola del equipo y las notificaciones"

                    binding.tvMyAssignedCount.text = state.myAssignedCount.toString()
                    binding.tvMyAssignedCount.visibility =
                        if (state.myAssignedCount > 0) View.VISIBLE else View.GONE

                    binding.tvTeamQueueCount.text = state.teamQueueCount.toString()
                    binding.tvTeamQueueCount.visibility =
                        if (state.teamQueueCount > 0) View.VISIBLE else View.GONE

                    binding.tvNotificationsCount.text = state.unreadNotificationsCount.toString()
                    binding.tvNotificationsCount.visibility =
                        if (state.unreadNotificationsCount > 0) View.VISIBLE else View.GONE

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
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