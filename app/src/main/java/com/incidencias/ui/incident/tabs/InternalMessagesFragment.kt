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
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.databinding.FragmentInternalMessagesBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.InternalMessageAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InternalMessagesFragment : Fragment(R.layout.fragment_internal_messages) {

    private var _binding: FragmentInternalMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: InternalMessageAdapter

    private var lastMessageCount = 0
    private var shouldForceScrollToBottom = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInternalMessagesBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserEmail = SessionManager(requireContext()).emailFlow.first().orEmpty()

            adapter = InternalMessageAdapter(
                currentUserEmail = currentUserEmail,
                items = emptyList()
            )

            binding.recyclerViewInternalMessages.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewInternalMessages.adapter = adapter

            binding.recyclerViewInternalMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    val total = adapter.itemCount

                    shouldForceScrollToBottom = total == 0 || lastVisible >= total - 2
                }
            })

            binding.btnSendInternalMessage.setOnClickListener {
                val message = binding.etNewInternalMessage.text?.toString()?.trim().orEmpty()
                if (message.isNotBlank()) {
                    shouldForceScrollToBottom = true
                    viewModel.sendInternalMessage(message)
                }
            }

            observeState()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val detail = state.detail ?: return@collect
                    val isClosed = detail.statusName.equals("CLOSED", ignoreCase = true)

                    val internalMessages = detail.messages.filter {
                        it.visibility.uppercase() == "INTERNAL"
                    }

                    val previousCount = lastMessageCount
                    adapter.updateData(internalMessages)
                    lastMessageCount = internalMessages.size

                    binding.progressBarInternalMessages.isVisible = state.isSendingInternalMessage

                    binding.etNewInternalMessage.isEnabled = !state.isSendingInternalMessage
                    binding.btnSendInternalMessage.isEnabled = !state.isSendingInternalMessage

                    if (isClosed) {
                        binding.etNewInternalMessage.visibility = View.GONE
                        binding.btnSendInternalMessage.visibility = View.GONE
                    } else {
                        binding.etNewInternalMessage.visibility = View.VISIBLE
                        binding.btnSendInternalMessage.visibility = View.VISIBLE
                    }

                    if (!state.isSendingInternalMessage) {
                        binding.etNewInternalMessage.text?.clear()
                    }

                    val hasNewMessages = internalMessages.size > previousCount
                    val shouldScrollNow = shouldForceScrollToBottom || previousCount == 0 || hasNewMessages

                    if (shouldScrollNow && internalMessages.isNotEmpty()) {
                        binding.recyclerViewInternalMessages.post {
                            binding.recyclerViewInternalMessages.scrollToPosition(internalMessages.lastIndex)
                        }
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