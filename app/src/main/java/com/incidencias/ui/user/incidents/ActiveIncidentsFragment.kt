package com.incidencias.ui.user.incidents

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.databinding.FragmentActiveIncidentsBinding
import com.incidencias.ui.common.adapter.IncidentAdapter
import kotlinx.coroutines.launch

class ActiveIncidentsFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadIncidents()
    }

    private fun loadIncidents() {
        lifecycleScope.launch {
            val repository = IncidentRepository(requireContext())
            val response = repository.getIncidents("OPEN", 0, 20)

            if (response.isSuccessful) {
                val items = response.body()?.content.orEmpty()
                binding.recyclerView.adapter = IncidentAdapter(items) { incident ->
                    val intent = android.content.Intent(requireContext(), com.incidencias.ui.incident.IncidentDetailActivity::class.java)
                    intent.putExtra(com.incidencias.ui.incident.IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}