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
import com.incidencias.databinding.FragmentPublicMessagesBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.PublicMessageAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class PublicMessagesFragment : Fragment(R.layout.fragment_public_messages) {

    private var _binding: FragmentPublicMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: PublicMessageAdapter

    private var lastMessageCount = 0
    private var shouldForceScrollToBottom = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPublicMessagesBinding.bind(view)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val keyboardBottom = (imeBottom - navBottom).coerceAtLeast(0)

            binding.layoutMessagesContainer.updatePadding(
                bottom = keyboardBottom
            )

            insets
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserEmail = SessionManager(requireContext()).emailFlow.first().orEmpty()

            adapter = PublicMessageAdapter(
                currentUserEmail = currentUserEmail,
                items = emptyList()
            )

            binding.recyclerViewMessages.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewMessages.adapter = adapter

            binding.recyclerViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    val total = adapter.itemCount

                    shouldForceScrollToBottom = total == 0 || lastVisible >= total - 2
                }
            })

            binding.btnSendMessage.setOnClickListener {
                val message = binding.etNewMessage.text?.toString()?.trim().orEmpty()
                if (message.isNotBlank()) {
                    shouldForceScrollToBottom = true
                    viewModel.sendPublicMessage(message)
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

                    val publicMessages = detail.messages.filter {
                        it.visibility.uppercase() == "PUBLIC"
                    }

                    val previousCount = lastMessageCount
                    adapter.updateData(publicMessages)
                    lastMessageCount = publicMessages.size

                    binding.progressBarMessages.isVisible = state.isSendingPublicMessage

                    binding.layoutComposer.visibility = if (isClosed) View.GONE else View.VISIBLE
                    binding.etNewMessage.isEnabled = !state.isSendingPublicMessage
                    binding.btnSendMessage.isEnabled = !state.isSendingPublicMessage

                    if (!state.isSendingPublicMessage) {
                        binding.etNewMessage.text?.clear()
                    }

                    val hasNewMessages = publicMessages.size > previousCount
                    val shouldScrollNow = shouldForceScrollToBottom || previousCount == 0 || hasNewMessages

                    if (shouldScrollNow && publicMessages.isNotEmpty()) {
                        binding.recyclerViewMessages.post {
                            binding.recyclerViewMessages.scrollToPosition(publicMessages.lastIndex)
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