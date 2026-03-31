package com.incidencias.ui.user.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.R
import com.incidencias.databinding.FragmentUserHomeBinding
import com.incidencias.ui.notifications.NotificationsFragment
import com.incidencias.ui.settings.SettingsActivity
import com.incidencias.ui.user.UserMainActivity
import com.incidencias.ui.user.incidents.ActiveIncidentsFragment
import com.incidencias.ui.user.incidents.IncidentHistoryFragment
import com.incidencias.ui.user.incidents.create.CreateIncidentActivity
import kotlinx.coroutines.launch

class UserHomeFragment : Fragment(R.layout.fragment_user_home) {

    private var _binding: FragmentUserHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserHomeBinding.bind(view)

        (requireActivity() as? UserMainActivity)?.setToolbarTitle("Portal usuario")

        setupClicks()
        observeUiState()

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
        binding.cardCreateIncident.setOnClickListener {
            startActivity(Intent(requireContext(), CreateIncidentActivity::class.java))
        }

        binding.cardActiveIncidents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.userFragmentContainer, ActiveIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.userFragmentContainer, IncidentHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.userFragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val firstName = state.firstName?.takeIf { it.isNotBlank() } ?: "usuario"

                    binding.tvWelcome.text = "Bienvenido, $firstName"
                    binding.tvSubtitle.text = "Consulta tus incidencias y notificaciones"

                    binding.tvActiveCount.text = state.activeIncidentsCount.toString()
                    binding.tvActiveCount.visibility =
                        if (state.activeIncidentsCount > 0) View.VISIBLE else View.GONE

                    binding.tvNotificationsCount.text = state.unreadNotificationsCount.toString()
                    binding.tvNotificationsCount.visibility =
                        if (state.unreadNotificationsCount > 0) View.VISIBLE else View.GONE

                    if (state.errorMessage != null) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
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