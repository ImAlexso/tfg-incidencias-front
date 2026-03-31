package com.incidencias.ui.incident.tabs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.databinding.FragmentSimpleListBinding
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.HistoryAdapter
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_simple_list) {

    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSimpleListBinding.bind(view)

        adapter = HistoryAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val detail = state.detail ?: return@collect
                    adapter.updateData(detail.events)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}