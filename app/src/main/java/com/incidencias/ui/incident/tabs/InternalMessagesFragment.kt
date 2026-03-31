package com.incidencias.ui.incident.tabs

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.databinding.FragmentInternalMessagesBinding
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.InternalMessageAdapter
import kotlinx.coroutines.launch

class InternalMessagesFragment : Fragment(R.layout.fragment_internal_messages) {

    private var _binding: FragmentInternalMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: InternalMessageAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInternalMessagesBinding.bind(view)

        adapter = InternalMessageAdapter(emptyList())
        binding.recyclerViewInternalMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewInternalMessages.adapter = adapter

        binding.btnSendInternalMessage.setOnClickListener {
            val message = binding.etNewInternalMessage.text?.toString()?.trim().orEmpty()
            viewModel.sendInternalMessage(message)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val detail = state.detail ?: return@collect

                    val internalMessages = detail.messages.filter {
                        it.visibility.uppercase() == "INTERNAL"
                    }

                    adapter.updateData(internalMessages)

                    binding.progressBarInternalMessages.isVisible = state.isSendingInternalMessage
                    binding.btnSendInternalMessage.isEnabled = !state.isSendingInternalMessage

                    if (!state.isSendingInternalMessage) {
                        binding.etNewInternalMessage.text?.clear()
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