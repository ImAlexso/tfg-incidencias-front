package com.incidencias.ui.incident

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.incidencias.data.remote.dto.incident.IncidentDetailResponse
import com.incidencias.data.repository.IncidentRepository
import com.incidencias.databinding.ActivityIncidentDetailBinding
import com.incidencias.session.SessionManager
import com.incidencias.ui.incident.tabs.AttachmentsFragment
import com.incidencias.ui.incident.tabs.HistoryFragment
import com.incidencias.ui.incident.tabs.InternalMessagesFragment
import com.incidencias.ui.incident.tabs.PublicMessagesFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import com.incidencias.data.repository.CatalogRepository

class IncidentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INCIDENT_ID = "extra_incident_id"
    }

    private lateinit var binding: ActivityIncidentDetailBinding
    private lateinit var sessionManager: SessionManager
    private var currentDetail: IncidentDetailResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncidentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        sessionManager = SessionManager(applicationContext)

        val incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1L)
        if (incidentId == -1L) {
            Toast.makeText(this, "Incidencia no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadIncidentDetail(incidentId)
    }

    private fun loadIncidentDetail(incidentId: Long) {
        lifecycleScope.launch {
            binding.progressBar.visibility = android.view.View.VISIBLE

            try {
                val repository = IncidentRepository(this@IncidentDetailActivity)
                val response = repository.getIncidentDetail(incidentId)

                binding.progressBar.visibility = android.view.View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val detail = response.body()!!
                    currentDetail = detail
                    bindHeader(detail)
                    setupTabs(detail)
                    setupTechnicianActions(detail)
                    setupManagerActions(detail)
                } else {
                    Toast.makeText(
                        this@IncidentDetailActivity,
                        "No se pudo cargar el detalle",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(
                    this@IncidentDetailActivity,
                    e.message ?: "Error al cargar el detalle",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun bindHeader(detail: IncidentDetailResponse) {
        binding.tvReference.text = detail.referenceCode
        binding.tvTitle.text = detail.title
        binding.tvDescription.text = detail.description
        binding.tvStatus.text = "Estado: ${detail.statusName}"
        binding.tvPriority.text = "Prioridad: ${detail.priorityName ?: "-"}"
        binding.tvCategory.text = "Categoría: ${detail.categoryName ?: "-"}"
        binding.tvTeam.text = "Equipo: ${detail.currentTeamName ?: "-"}"
    }

    private fun setupTabs(detail: IncidentDetailResponse) {
        lifecycleScope.launch {
            val role = sessionManager.roleFlow.first().orEmpty().uppercase()

            val fragments = mutableListOf<Pair<String, androidx.fragment.app.Fragment>>()

            fragments.add("Públicos" to PublicMessagesFragment.newInstance(detail.id, detail.messages))

            if (role == "TECHNICIAN" || role == "MANAGER" || role == "ADMIN") {
                fragments.add("Internos" to InternalMessagesFragment.newInstance(detail.id, detail.messages))
            }

            fragments.add("Adjuntos" to AttachmentsFragment.newInstance(detail.id, detail.attachments))
            fragments.add("Historial" to HistoryFragment.newInstance(detail.events))

            binding.viewPager.adapter = object : FragmentStateAdapter(this@IncidentDetailActivity) {
                override fun getItemCount(): Int = fragments.size
                override fun createFragment(position: Int) = fragments[position].second
            }

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = fragments[position].first
            }.attach()
        }
    }

    private fun setupTechnicianActions(detail: IncidentDetailResponse) {
        lifecycleScope.launch {
            val role = sessionManager.roleFlow.first().orEmpty().uppercase()
            val userId = sessionManager.userIdFlow.first()

            if (role == "TECHNICIAN") {
                binding.layoutTechnicianActions.visibility = android.view.View.VISIBLE

                binding.btnAssignToMe.setOnClickListener {
                    if (userId != null) {
                        lifecycleScope.launch {
                            val repository = IncidentRepository(this@IncidentDetailActivity)
                            val response = repository.assignTechnician(detail.id, userId)

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@IncidentDetailActivity,
                                    "Incidencia asignada",
                                    Toast.LENGTH_SHORT
                                ).show()
                                recreate()
                            } else {
                                Toast.makeText(
                                    this@IncidentDetailActivity,
                                    "No se pudo asignar",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                binding.btnResolveIncident.setOnClickListener {
                    lifecycleScope.launch {
                        val repository = IncidentRepository(this@IncidentDetailActivity)
                        val response = repository.resolveIncident(detail.id)

                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@IncidentDetailActivity,
                                "Incidencia resuelta",
                                Toast.LENGTH_SHORT
                            ).show()
                            recreate()
                        } else {
                            Toast.makeText(
                                this@IncidentDetailActivity,
                                "No se pudo resolver",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                binding.layoutTechnicianActions.visibility = android.view.View.GONE
            }
        }
    }
    private fun setupManagerActions(detail: IncidentDetailResponse) {
        lifecycleScope.launch {
            val role = sessionManager.roleFlow.first().orEmpty().uppercase()

            if (role == "MANAGER" || role == "ADMIN") {
                binding.layoutManagerActions.visibility = android.view.View.VISIBLE

                binding.btnChangePriority.setOnClickListener {
                    lifecycleScope.launch {
                        val catalogRepository = CatalogRepository(this@IncidentDetailActivity)
                        val incidentRepository = IncidentRepository(this@IncidentDetailActivity)
                        val prioritiesResponse = catalogRepository.getPriorities()

                        if (prioritiesResponse.isSuccessful) {
                            val priorities = prioritiesResponse.body().orEmpty().filter { it.active }
                            val names = priorities.map { it.name }.toTypedArray()

                            AlertDialog.Builder(this@IncidentDetailActivity)
                                .setTitle("Selecciona prioridad")
                                .setItems(names) { _, which ->
                                    lifecycleScope.launch {
                                        val selected = priorities[which]
                                        val response = incidentRepository.updateIncidentPriority(detail.id, selected.id)
                                        if (response.isSuccessful) {
                                            Toast.makeText(this@IncidentDetailActivity, "Prioridad actualizada", Toast.LENGTH_SHORT).show()
                                            recreate()
                                        } else {
                                            Toast.makeText(this@IncidentDetailActivity, "No se pudo actualizar", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .show()
                        }
                    }
                }

                binding.btnChangeTeam.setOnClickListener {
                    lifecycleScope.launch {
                        val catalogRepository = CatalogRepository(this@IncidentDetailActivity)
                        val incidentRepository = IncidentRepository(this@IncidentDetailActivity)
                        val teamsResponse = catalogRepository.getTeams()

                        if (teamsResponse.isSuccessful) {
                            val teams = teamsResponse.body().orEmpty().filter { it.active }
                            val names = teams.map { it.name }.toTypedArray()

                            AlertDialog.Builder(this@IncidentDetailActivity)
                                .setTitle("Selecciona equipo")
                                .setItems(names) { _, which ->
                                    lifecycleScope.launch {
                                        val selected = teams[which]
                                        val response = incidentRepository.updateIncidentTeam(detail.id, selected.id)
                                        if (response.isSuccessful) {
                                            Toast.makeText(this@IncidentDetailActivity, "Equipo actualizado", Toast.LENGTH_SHORT).show()
                                            recreate()
                                        } else {
                                            Toast.makeText(this@IncidentDetailActivity, "No se pudo actualizar", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .show()
                        }
                    }
                }

                binding.btnAssignTechnicianManager.setOnClickListener {
                    lifecycleScope.launch {
                        val incidentRepository = IncidentRepository(this@IncidentDetailActivity)
                        val response = incidentRepository.getAssignableTechnicians(detail.id)

                        if (response.isSuccessful) {
                            val technicians = response.body().orEmpty()
                            val names = technicians.map { "${it.firstName} ${it.lastName} (${it.email})" }.toTypedArray()

                            AlertDialog.Builder(this@IncidentDetailActivity)
                                .setTitle("Asignar técnico")
                                .setItems(names) { _, which ->
                                    lifecycleScope.launch {
                                        val selected = technicians[which]
                                        val assignResponse = incidentRepository.assignTechnician(detail.id, selected.id)
                                        if (assignResponse.isSuccessful) {
                                            Toast.makeText(this@IncidentDetailActivity, "Técnico asignado", Toast.LENGTH_SHORT).show()
                                            recreate()
                                        } else {
                                            Toast.makeText(this@IncidentDetailActivity, "No se pudo asignar", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .show()
                        }
                    }
                }

                binding.btnResolveIncidentManager.setOnClickListener {
                    lifecycleScope.launch {
                        val incidentRepository = IncidentRepository(this@IncidentDetailActivity)
                        val response = incidentRepository.resolveIncident(detail.id)

                        if (response.isSuccessful) {
                            Toast.makeText(this@IncidentDetailActivity, "Incidencia resuelta", Toast.LENGTH_SHORT).show()
                            recreate()
                        } else {
                            Toast.makeText(this@IncidentDetailActivity, "No se pudo resolver", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                binding.btnCloseIncident.setOnClickListener {
                    lifecycleScope.launch {
                        val incidentRepository = IncidentRepository(this@IncidentDetailActivity)
                        val response = incidentRepository.closeIncident(detail.id)

                        if (response.isSuccessful) {
                            Toast.makeText(this@IncidentDetailActivity, "Incidencia cerrada", Toast.LENGTH_SHORT).show()
                            recreate()
                        } else {
                            Toast.makeText(this@IncidentDetailActivity, "No se pudo cerrar", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                binding.layoutManagerActions.visibility = android.view.View.GONE
            }
        }
    }
}