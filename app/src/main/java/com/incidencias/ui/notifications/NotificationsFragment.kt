package com.incidencias.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.incidencias.R
import com.incidencias.databinding.FragmentNotificationsBinding
import com.incidencias.ui.incident.IncidentDetailActivity
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        setupRecycler()
        setupFilters()
        setupActions()
        observeUiState()

        if (savedInstanceState == null) {
            viewModel.loadNotifications()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.loadNotifications(forceRefresh = true)
        }
    }

    private fun setupRecycler() {
        adapter = NotificationAdapter(
            onClick = { notification ->
                val openDetail = {
                    val incidentId = notification.incidentId
                    if (incidentId != null) {
                        val intent = Intent(requireContext(), IncidentDetailActivity::class.java)
                        intent.putExtra(IncidentDetailActivity.EXTRA_INCIDENT_ID, incidentId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Esta notificación no tiene una incidencia asociada",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                if (!notification.read) {
                    viewModel.markAsRead(notification.id) {
                        openDetail()
                    }
                } else {
                    openDetail()
                }
            },
            onDeleteClick = { notification ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Borrar notificación")
                    .setMessage("¿Quieres borrar esta notificación?")
                    .setPositiveButton("Sí") { _, _ ->
                        viewModel.deleteNotification(notification.id)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.layoutManager = layoutManager
        binding.recyclerViewNotifications.adapter = adapter

        binding.recyclerViewNotifications.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalItems = layoutManager.itemCount

                if (lastVisible >= totalItems - 4) {
                    viewModel.loadMoreNotifications()
                }
            }
        })
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(NotificationFilter.ALL)
        }

        binding.chipUnread.setOnClickListener {
            viewModel.setFilter(NotificationFilter.UNREAD)
        }

        binding.chipCreated.setOnClickListener {
            viewModel.setFilter(NotificationFilter.INCIDENT_CREATED)
        }

        binding.chipStatus.setOnClickListener {
            viewModel.setFilter(NotificationFilter.STATUS_CHANGED)
        }

        binding.chipMessages.setOnClickListener {
            viewModel.setFilter(NotificationFilter.MESSAGE_PUBLIC)
        }

        binding.chipAttachments.setOnClickListener {
            viewModel.setFilter(NotificationFilter.ATTACHMENT_UPLOADED)
        }
    }

    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications(forceRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadNotifications()
        }

        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.swipeRefresh.isRefreshing = state.isRefreshing

                    binding.recyclerViewNotifications.visibility =
                        if (state.items.isNotEmpty()) View.VISIBLE else View.GONE

                    binding.layoutEmpty.visibility =
                        if (state.items.isEmpty() && !state.isLoading && state.errorMessage == null) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.layoutError.visibility =
                        if (state.errorMessage != null && !state.isLoading && state.items.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.tvError.text = state.errorMessage ?: ""
                    binding.tvUnreadCount.text = "No leídas: ${state.unreadCount}"
                    binding.btnMarkAllRead.isEnabled =
                        state.unreadCount > 0 && !state.isLoading && !state.isRefreshing

                    adapter.submitList(state.items)

                    when (state.selectedFilter) {
                        NotificationFilter.ALL -> binding.chipGroup.check(binding.chipAll.id)
                        NotificationFilter.UNREAD -> binding.chipGroup.check(binding.chipUnread.id)
                        NotificationFilter.INCIDENT_CREATED -> binding.chipGroup.check(binding.chipCreated.id)
                        NotificationFilter.STATUS_CHANGED -> binding.chipGroup.check(binding.chipStatus.id)
                        NotificationFilter.MESSAGE_PUBLIC -> binding.chipGroup.check(binding.chipMessages.id)
                        NotificationFilter.ATTACHMENT_UPLOADED -> binding.chipGroup.check(binding.chipAttachments.id)
                    }

                    if (state.errorMessage != null && state.items.isNotEmpty()) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
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
