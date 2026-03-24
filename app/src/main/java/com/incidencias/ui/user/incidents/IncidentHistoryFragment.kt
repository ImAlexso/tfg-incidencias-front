package com.incidencias.ui.user.incidents

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

class IncidentHistoryFragment : Fragment(R.layout.fragment_active_incidents) {

    private var _binding: FragmentActiveIncidentsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActiveIncidentsBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val repository = IncidentRepository(requireContext())
            val response = repository.getIncidents("CLOSED", 0, 20)

            if (response.isSuccessful) {
                val items = response.body()?.content.orEmpty()
                binding.recyclerView.adapter = IncidentAdapter(items) { incident ->
                    val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
                    intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incident.id)
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