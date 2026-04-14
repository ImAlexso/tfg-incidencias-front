package com.incidencias.ui.incident

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.incidencias.databinding.BottomSheetIncidentActionsBinding
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.incidencias.databinding.BottomSheetSelectionListBinding
import com.incidencias.databinding.ItemBottomSheetOptionBinding
import com.incidencias.ui.incident.model.SelectionSheetOption

class IncidentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INCIDENT_ID = "extra_incident_id"
    }

    private lateinit var binding: ActivityIncidentDetailBinding
    private val viewModel: IncidentDetailViewModel by viewModels()

    private var tabsInitialized = false
    private var detailsExpanded = false

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

        setupDetailsToggle()
        observeViewModel()
        viewModel.initialize(incidentId)
    }

    private fun setupDetailsToggle() {
        binding.layoutDetailsHeader.setOnClickListener {
            detailsExpanded = !detailsExpanded
            renderDetailsExpanded()
        }
        renderDetailsExpanded()
    }

    private fun renderDetailsExpanded() {
        binding.layoutDetailsBody.visibility = if (detailsExpanded) View.VISIBLE else View.GONE
        binding.ivDetailsChevron.animate()
            .rotation(if (detailsExpanded) 180f else 0f)
            .setDuration(180)
            .start()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        val showInitialLoading = state.isLoading && state.detail == null
                        binding.layoutLoading.visibility = if (showInitialLoading) View.VISIBLE else View.GONE

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

                            is IncidentDetailEvent.CloseScreen -> finish()

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
        binding.tvTeam.text = "Equipo: ${detail.currentTeamName ?: "-"}"
        binding.tvTechnician.text = "Técnico: ${detail.assignedTechnicianName ?: "Sin asignar"}"

        val statusRaw = detail.statusName.uppercase()
        val priorityRaw = detail.priorityName?.uppercase()

        binding.tvStatus.text = when (statusRaw) {
            "OPEN" -> "Abierta"
            "IN_PROGRESS" -> "En progreso"
            "RESOLVED" -> "Resuelta"
            "CLOSED" -> "Cerrada"
            else -> detail.statusName
        }

        binding.tvPriority.text = when (priorityRaw) {
            "CRITICAL" -> "Crítica"
            "HIGH" -> "Alta"
            "MEDIUM" -> "Media"
            "LOW" -> "Baja"
            else -> detail.priorityName ?: "-"
        }

        binding.tvCategory.text = detail.categoryName ?: "-"

        when (statusRaw) {
            "OPEN" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
                binding.tvStatus.setTextColor(getColor(R.color.status_open_text))
            }
            "IN_PROGRESS" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                binding.tvStatus.setTextColor(getColor(R.color.status_in_progress_text))
            }
            "RESOLVED" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
                binding.tvStatus.setTextColor(getColor(R.color.status_resolved_text))
            }
            "CLOSED" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
                binding.tvStatus.setTextColor(getColor(R.color.status_closed_text))
            }
            else -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_open)
                binding.tvStatus.setTextColor(getColor(R.color.status_open_text))
            }
        }

        when (priorityRaw) {
            "CRITICAL" -> {
                binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_critical)
                binding.tvPriority.setTextColor(getColor(R.color.priority_critical_text))
            }
            "HIGH" -> {
                binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_high)
                binding.tvPriority.setTextColor(getColor(R.color.priority_high_text))
            }
            "MEDIUM" -> {
                binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_medium)
                binding.tvPriority.setTextColor(getColor(R.color.priority_medium_text))
            }
            "LOW" -> {
                binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
                binding.tvPriority.setTextColor(getColor(R.color.priority_low_text))
            }
            else -> {
                binding.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
                binding.tvPriority.setTextColor(getColor(R.color.priority_low_text))
            }
        }
    }

    private fun bindRoleActions(state: IncidentDetailUiState) {
        val detail = state.detail ?: return
        val role = state.role.uppercase()

        binding.layoutActionBar.visibility = View.GONE
        binding.btnPrimaryAction.setOnClickListener(null)

        val canStartProgress = viewModel.canStartProgress()
        val isTechnicianInProgress = detail.statusName.equals("IN_PROGRESS", ignoreCase = true)

        if (role == "TECHNICIAN") {
            when {
                detail.canAssignToMe -> {
                    binding.layoutActionBar.visibility = View.VISIBLE
                    binding.ivActionIcon.setImageResource(R.drawable.ic_role_support_small)
                    binding.ivActionIcon.setColorFilter(getColor(R.color.primary))
                    binding.tvActionTitle.text = "Incidencia sin asignar"
                    binding.tvActionSubtitle.text = "Puedes asignártela y empezar a trabajar en ella."
                    binding.btnPrimaryAction.text = "Asignarme"
                    binding.btnPrimaryAction.setOnClickListener {
                        viewModel.assignToMe()
                    }
                }

                canStartProgress -> {
                    binding.layoutActionBar.visibility = View.VISIBLE
                    binding.ivActionIcon.setImageResource(R.drawable.ic_role_support_small)
                    binding.ivActionIcon.setColorFilter(getColor(R.color.primary))
                    binding.tvActionTitle.text = "Lista para empezar"
                    binding.tvActionSubtitle.text = "La incidencia ya está asignada a ti."
                    binding.btnPrimaryAction.text = "Empezar"
                    binding.btnPrimaryAction.setOnClickListener {
                        viewModel.startProgress()
                    }
                }

                detail.canResolve && isTechnicianInProgress -> {
                    binding.layoutActionBar.visibility = View.VISIBLE
                    binding.ivActionIcon.setImageResource(R.drawable.ic_role_support_small)
                    binding.ivActionIcon.setColorFilter(getColor(R.color.success))
                    binding.tvActionTitle.text = "Incidencia en progreso"
                    binding.tvActionSubtitle.text = "Puedes marcarla como resuelta cuando termines."
                    binding.btnPrimaryAction.text = "Resolver"
                    binding.btnPrimaryAction.setOnClickListener {
                        viewModel.resolveIncident()
                    }
                }
            }
            return
        }

        if (role == "MANAGER" || role == "ADMIN") {
            val hasAnyManagerAction =
                detail.canChangePriority ||
                        detail.canChangeTeam ||
                        detail.canAssignTechnician ||
                        detail.canResolve ||
                        detail.canClose

            if (hasAnyManagerAction) {
                binding.layoutActionBar.visibility = View.VISIBLE
                binding.ivActionIcon.setImageResource(R.drawable.ic_role_manager_small)
                binding.ivActionIcon.setColorFilter(getColor(R.color.badge_internal))
                binding.tvActionTitle.text = "Gestión de incidencia"
                binding.tvActionSubtitle.text = "Prioridad, equipo, asignación y cierre."
                binding.btnPrimaryAction.text = "Gestionar"
                binding.btnPrimaryAction.setOnClickListener {
                    showManagerActionsBottomSheet(detail)
                }
            }
        }
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

        val options = priorities.map {
            SelectionSheetOption(
                id = it.id,
                title = mapPriorityName(it.name),
                subtitle = mapPrioritySubtitle(it.name),
                iconRes = R.drawable.ic_history_flag_small,
                tintRes = mapPriorityTint(it.name)
            )
        }

        showSelectionBottomSheet(
            title = "Selecciona prioridad",
            subtitle = "Ajusta la prioridad de la incidencia.",
            options = options
        ) { selectedId ->
            viewModel.updatePriority(selectedId)
        }
    }

    private fun showTeamDialog(teams: List<TeamResponse>) {
        if (teams.isEmpty()) {
            Toast.makeText(this, "No hay equipos disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val options = teams.map {
            SelectionSheetOption(
                id = it.id,
                title = mapTeamName(it.name),
                subtitle = "Reasigna la incidencia a este equipo.",
                iconRes = R.drawable.ic_home_team,
                tintRes = R.color.primary
            )
        }

        showSelectionBottomSheet(
            title = "Selecciona equipo",
            subtitle = "Elige el equipo responsable de la incidencia.",
            options = options
        ) { selectedId ->
            viewModel.updateTeam(selectedId)
        }
    }

    private fun showTechnicianDialog(technicians: List<AssignableTechnicianResponse>) {
        if (technicians.isEmpty()) {
            Toast.makeText(this, "No hay técnicos disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val options = technicians.map {
            SelectionSheetOption(
                id = it.id,
                title = "${it.firstName} ${it.lastName}",
                subtitle = it.email,
                iconRes = R.drawable.ic_role_support_small,
                tintRes = R.color.success
            )
        }

        showSelectionBottomSheet(
            title = "Asignar técnico",
            subtitle = "Selecciona un técnico disponible para esta incidencia.",
            options = options
        ) { selectedId ->
            viewModel.assignTechnician(selectedId)
        }
    }
    private fun showManagerActionsBottomSheet(detail: IncidentDetailResponse) {
        val sheetBinding = BottomSheetIncidentActionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.skipCollapsed = true

        bindManagerSheetAction(
            view = sheetBinding.actionPriority,
            visible = detail.canChangePriority
        ) {
            dialog.dismiss()
            viewModel.requestPriorityOptions()
        }

        bindManagerSheetAction(
            view = sheetBinding.actionTeam,
            visible = detail.canChangeTeam
        ) {
            dialog.dismiss()
            viewModel.requestTeamOptions()
        }

        bindManagerSheetAction(
            view = sheetBinding.actionAssignTechnician,
            visible = detail.canAssignTechnician
        ) {
            dialog.dismiss()
            viewModel.requestAssignableTechnicians()
        }

        bindManagerSheetAction(
            view = sheetBinding.actionResolve,
            visible = detail.canResolve
        ) {
            dialog.dismiss()
            viewModel.resolveIncident()
        }

        bindManagerSheetAction(
            view = sheetBinding.actionClose,
            visible = detail.canClose
        ) {
            dialog.dismiss()
            viewModel.closeIncident()
        }

        val hasDangerSection = detail.canClose
        sheetBinding.dividerDanger.visibility = if (hasDangerSection) View.VISIBLE else View.GONE

        dialog.show()
    }

    private fun bindManagerSheetAction(
        view: View,
        visible: Boolean,
        onClick: () -> Unit
    ) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
        view.setOnClickListener(if (visible) View.OnClickListener { onClick() } else null)
    }
    private fun showSelectionBottomSheet(
        title: String,
        subtitle: String,
        options: List<SelectionSheetOption>,
        onSelected: (Long) -> Unit
    ) {
        val sheetBinding = BottomSheetSelectionListBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.skipCollapsed = true

        sheetBinding.tvSheetTitle.text = title
        sheetBinding.tvSheetSubtitle.text = subtitle
        sheetBinding.containerOptions.removeAllViews()

        options.forEach { option ->
            val itemBinding = ItemBottomSheetOptionBinding.inflate(layoutInflater)

            itemBinding.tvOptionTitle.text = option.title
            itemBinding.tvOptionSubtitle.text = option.subtitle
            itemBinding.ivOptionIcon.setImageResource(option.iconRes)
            itemBinding.ivOptionIcon.setColorFilter(
                ContextCompat.getColor(this, option.tintRes)
            )

            itemBinding.root.setOnClickListener {
                dialog.dismiss()
                onSelected(option.id)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            itemBinding.root.layoutParams = params

            sheetBinding.containerOptions.addView(itemBinding.root)
        }

        dialog.show()
    }

    private fun mapTeamName(raw: String): String {
        return when (raw.uppercase()) {
            "IT_SUPPORT" -> "IT Support"
            "DEVOPS" -> "DevOps"
            "NETWORK" -> "Network"
            "SYSTEMS" -> "Systems"
            else -> raw.replace("_", " ")
        }
    }

    private fun mapPriorityName(raw: String): String {
        return when (raw.uppercase()) {
            "LOW" -> "Baja"
            "MEDIUM" -> "Media"
            "HIGH" -> "Alta"
            "CRITICAL" -> "Crítica"
            else -> raw
        }
    }

    private fun mapPrioritySubtitle(raw: String): String {
        return when (raw.uppercase()) {
            "LOW" -> "Impacto bajo o poco urgente."
            "MEDIUM" -> "Prioridad estándar de trabajo."
            "HIGH" -> "Requiere atención prioritaria."
            "CRITICAL" -> "Impacto crítico o bloqueo importante."
            else -> "Selecciona esta prioridad."
        }
    }

    private fun mapPriorityTint(raw: String): Int {
        return when (raw.uppercase()) {
            "LOW" -> R.color.priority_low_text
            "MEDIUM" -> R.color.priority_medium_text
            "HIGH" -> R.color.priority_high_text
            "CRITICAL" -> R.color.priority_critical_text
            else -> R.color.warning
        }
    }
}