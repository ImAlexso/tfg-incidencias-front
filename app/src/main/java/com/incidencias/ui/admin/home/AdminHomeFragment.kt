package com.incidencias.ui.admin.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.incidencias.R
import com.incidencias.databinding.FragmentAdminHomeBinding
import com.incidencias.ui.admin.incidents.AdminIncidentsFragment
import com.incidencias.ui.admin.incidents.AdminResolvedFragment
import com.incidencias.ui.settings.SettingsActivity

class AdminHomeFragment : Fragment(R.layout.fragment_admin_home) {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminHomeBinding.bind(view)

        binding.cardGlobalIncidents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, AdminIncidentsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardResolvedIncidents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, AdminResolvedFragment())
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