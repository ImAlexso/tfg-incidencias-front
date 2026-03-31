package com.incidencias.ui.admin.incidents

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import com.incidencias.ui.incident.IncidentDetailActivity
import kotlinx.coroutines.launch

class AdminResolvedFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadResolvedIncidents()
    }

    private fun loadResolvedIncidents() {
        lifecycleScope.launch {
            val repository = IncidentRepository(requireContext())

            val resolvedResponse = repository.getIncidents("RESOLVED", 0, 100)
            val closedResponse = repository.getIncidents("CLOSED", 0, 100)

            val items = mutableListOf<com.incidencias.data.remote.dto.incident.IncidentListItemResponse>()
            if (resolvedResponse.isSuccessful) items.addAll(resolvedResponse.body()?.content.orEmpty())
            if (closedResponse.isSuccessful) items.addAll(closedResponse.body()?.content.orEmpty())

           /* binding.recyclerView.adapter = IncidentAdapter(items) { incident ->
                val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
                intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
                startActivity(intent)
            }*/
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}