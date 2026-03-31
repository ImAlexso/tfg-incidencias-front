package com.incidencias.ui.incident

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.incidencias.R
import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.data.remote.dto.incident.AssignableTechnicianResponse
import com.incidencias.data.remote.dto.incident.IncidentDetailResponse
import com.incidencias.databinding.ActivityIncidentDetailBinding
import com.incidencias.ui.incident.tabs.AttachmentsFragment
import com.incidencias.ui.incident.tabs.HistoryFragment
import com.incidencias.ui.incident.tabs.InternalMessagesFragment
import com.incidencias.ui.incident.tabs.PublicMessagesFragment
import kotlinx.coroutines.launch

class IncidentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INCIDENT_ID = "extra_incident_id"
    }

    private lateinit var binding: ActivityIncidentDetailBinding
    private val viewModel: IncidentDetailViewModel by viewModels()

    private var tabsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncidentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1L)
        if (incidentId == -1L) {
            Toast.makeText(this, "Incidencia no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupActions()
        observeViewModel()
        viewModel.initialize(incidentId)
    }

    private fun setupActions() {
        binding.btnAssignToMe.setOnClickListener {
            viewModel.assignToMe()
        }

        binding.btnStartProgress.setOnClickListener {
            viewModel.startProgress()
        }

        binding.btnResolveIncident.setOnClickListener {
            viewModel.resolveIncident()
        }

        binding.btnChangePriority.setOnClickListener {
            viewModel.requestPriorityOptions()
        }

        binding.btnChangeTeam.setOnClickListener {
            viewModel.requestTeamOptions()
        }

        binding.btnAssignTechnicianManager.setOnClickListener {
            viewModel.requestAssignableTechnicians()
        }

        binding.btnResolveIncidentManager.setOnClickListener {
            viewModel.resolveIncident()
        }

        binding.btnCloseIncident.setOnClickListener {
            viewModel.closeIncident()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        val showInitialLoading = state.isLoading && state.detail == null

                        binding.progressBar.visibility =
                            if (showInitialLoading) View.VISIBLE else View.GONE

                        val detail = state.detail
                        if (detail != null) {
                            bindHeader(detail)
                            bindRoleActions(state)
                            showContent(true)

                            if (!tabsInitialized) {
                                setupTabs(state.role)
                                tabsInitialized = true
                            }
                        } else {
                            showContent(false)
                        }

                        state.errorMessage?.let { message ->
                            Toast.makeText(this@IncidentDetailActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is IncidentDetailEvent.ShowMessage -> {
                                Toast.makeText(
                                    this@IncidentDetailActivity,
                                    event.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is IncidentDetailEvent.CloseScreen -> {
                                finish()
                            }

                            is IncidentDetailEvent.ShowPriorityPicker -> {
                                showPriorityDialog(event.priorities)
                            }

                            is IncidentDetailEvent.ShowTeamPicker -> {
                                showTeamDialog(event.teams)
                            }

                            is IncidentDetailEvent.ShowTechnicianPicker -> {
                                showTechnicianDialog(event.technicians)
                            }

                            is IncidentDetailEvent.OpenDownloadUrl -> {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.url)))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showContent(show: Boolean) {
        binding.detailContentContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun bindHeader(detail: IncidentDetailResponse) {
        binding.tvReference.text = detail.referenceCode
        binding.tvTitle.text = detail.title
        binding.tvDescription.text = detail.description
        binding.tvStatus.text = detail.statusName
        binding.tvPriority.text = detail.priorityName ?: "-"
        binding.tvCategory.text = detail.categoryName ?: "-"
        binding.tvTeam.text = "Equipo: ${detail.currentTeamName ?: "-"}"
        binding.tvTechnician.text = "Técnico: ${detail.assignedTechnicianName ?: "Sin asignar"}"

        when (detail.statusName.uppercase()) {
            "OPEN" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
            "IN_PROGRESS" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            "RESOLVED" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
            "CLOSED" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
            else -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
        }

        when (detail.priorityName?.uppercase()) {
            "CRITICAL" -> binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_critical)
            "HIGH" -> binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_high)
            "MEDIUM" -> binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_medium)
            "LOW" -> binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
            else -> binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
        }
    }

    private fun bindRoleActions(state: IncidentDetailUiState) {
        val detail = state.detail ?: return

        val canStartProgress = viewModel.canStartProgress()
        val isTechnicianInProgress = detail.statusName.equals("IN_PROGRESS", ignoreCase = true)

        val showTechnicianLayout =
            state.role == "TECHNICIAN" &&
                    (detail.canAssignToMe || canStartProgress || (detail.canResolve && isTechnicianInProgress))

        binding.layoutTechnicianActions.visibility =
            if (showTechnicianLayout) View.VISIBLE else View.GONE

        binding.btnAssignToMe.visibility =
            if (detail.canAssignToMe) View.VISIBLE else View.GONE

        binding.btnStartProgress.visibility =
            if (canStartProgress) View.VISIBLE else View.GONE

        binding.btnResolveIncident.visibility =
            if (detail.canResolve && state.role == "TECHNICIAN" && isTechnicianInProgress) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val showManagerLayout =
            (state.role == "MANAGER" || state.role == "ADMIN") &&
                    (detail.canChangePriority ||
                            detail.canChangeTeam ||
                            detail.canAssignTechnician ||
                            detail.canResolve ||
                            detail.canClose)

        binding.layoutManagerActions.visibility =
            if (showManagerLayout) View.VISIBLE else View.GONE

        binding.btnChangePriority.visibility =
            if (detail.canChangePriority) View.VISIBLE else View.GONE

        binding.btnChangeTeam.visibility =
            if (detail.canChangeTeam) View.VISIBLE else View.GONE

        binding.btnAssignTechnicianManager.visibility =
            if (detail.canAssignTechnician) View.VISIBLE else View.GONE

        binding.btnResolveIncidentManager.visibility =
            if (detail.canResolve && (state.role == "MANAGER" || state.role == "ADMIN")) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnCloseIncident.visibility =
            if (detail.canClose) View.VISIBLE else View.GONE
    }

    private fun setupTabs(role: String) {
        val fragments = mutableListOf<Pair<String, androidx.fragment.app.Fragment>>()

        fragments.add("Mensajes" to PublicMessagesFragment())

        if (role == "TECHNICIAN" || role == "MANAGER" || role == "ADMIN") {
            fragments.add("Internos" to InternalMessagesFragment())
        }

        fragments.add("Adjuntos" to AttachmentsFragment())
        fragments.add("Historial" to HistoryFragment())

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int) = fragments[position].second
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = fragments[position].first
        }.attach()
    }

    private fun showPriorityDialog(priorities: List<PriorityResponse>) {
        if (priorities.isEmpty()) {
            Toast.makeText(this, "No hay prioridades disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val names = priorities.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona prioridad")
            .setItems(names) { _, which ->
                viewModel.updatePriority(priorities[which].id)
            }
            .show()
    }

    private fun showTeamDialog(teams: List<TeamResponse>) {
        if (teams.isEmpty()) {
            Toast.makeText(this, "No hay equipos disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val names = teams.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona equipo")
            .setItems(names) { _, which ->
                viewModel.updateTeam(teams[which].id)
            }
            .show()
    }

    private fun showTechnicianDialog(technicians: List<AssignableTechnicianResponse>) {
        if (technicians.isEmpty()) {
            Toast.makeText(this, "No hay técnicos disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val names = technicians.map {
            "${it.firstName} ${it.lastName} (${it.email})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar técnico")
            .setItems(names) { _, which ->
                viewModel.assignTechnician(technicians[which].id)
            }
            .show()
    }
}