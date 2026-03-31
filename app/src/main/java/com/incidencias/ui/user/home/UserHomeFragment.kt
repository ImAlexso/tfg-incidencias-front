package com.incidencias.ui.user.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.incidencias.R
import com.incidencias.databinding.FragmentUserHomeBinding
import com.incidencias.ui.notifications.NotificationsFragment
import com.incidencias.ui.settings.SettingsActivity
import com.incidencias.ui.user.UserMainActivity
import com.incidencias.ui.user.incidents.ActiveIncidentsFragment
import com.incidencias.ui.user.incidents.IncidentHistoryFragment
import com.incidencias.ui.user.incidents.create.CreateIncidentActivity

class UserHomeFragment : Fragment(R.layout.fragment_user_home) {

    private var _binding: FragmentUserHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserHomeBinding.bind(view)

        (requireActivity() as? UserMainActivity)?.setToolbarTitle("Portal usuario")

        setupClicks()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}