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
import com.incidencias.databinding.FragmentPublicMessagesBinding
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.PublicMessageAdapter
import kotlinx.coroutines.launch

class PublicMessagesFragment : Fragment(R.layout.fragment_public_messages) {

    private var _binding: FragmentPublicMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: PublicMessageAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPublicMessagesBinding.bind(view)

        adapter = PublicMessageAdapter(emptyList())
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMessages.adapter = adapter

        binding.btnSendMessage.setOnClickListener {
            val message = binding.etNewMessage.text?.toString()?.trim().orEmpty()
            viewModel.sendPublicMessage(message)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val detail = state.detail ?: return@collect
                    val isClosed = detail.statusName.equals("CLOSED", ignoreCase = true)

                    val publicMessages = detail.messages.filter {
                        it.visibility.uppercase() == "PUBLIC"
                    }

                    adapter.updateData(publicMessages)

                    binding.progressBarMessages.isVisible = state.isSendingPublicMessage

                    binding.etNewMessage.isEnabled = !isClosed && !state.isSendingPublicMessage
                    binding.btnSendMessage.isEnabled = !isClosed && !state.isSendingPublicMessage

                    binding.etNewMessage.visibility = if (isClosed) View.GONE else View.VISIBLE
                    binding.btnSendMessage.visibility = if (isClosed) View.GONE else View.VISIBLE

                    if (!state.isSendingPublicMessage) {
                        binding.etNewMessage.text?.clear()
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