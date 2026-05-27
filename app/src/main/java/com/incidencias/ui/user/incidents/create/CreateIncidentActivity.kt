package com.incidencias.ui.user.incidents.create

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.incidencias.data.remote.dto.catalog.CategoryResponse
import com.incidencias.data.remote.dto.catalog.PriorityResponse
import com.incidencias.data.remote.dto.catalog.TeamResponse
import com.incidencias.databinding.ActivityCreateIncidentBinding
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class CreateIncidentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateIncidentBinding
    private val viewModel: CreateIncidentViewModel by viewModels()

    private var categories: List<CategoryResponse> = emptyList()
    private var priorities: List<PriorityResponse> = emptyList()
    private var teams: List<TeamResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateIncidentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupKeyboardInsets()

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupModeSelector()
        setupListeners()
        observeViewModel()
        viewModel.loadCatalogs()
    }

    private fun setupModeSelector() {
        binding.rgMode.setOnCheckedChangeListener { _, checkedId ->
            val useAi = checkedId == binding.rbWithAi.id
            binding.layoutManualFields.visibility = if (useAi) View.GONE else View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnCreateIncident.setOnClickListener {
            val useAi = binding.rbWithAi.isChecked
            val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            val description = binding.etDescription.text?.toString()?.trim().orEmpty()

            val selectedCategoryId = if (!useAi && categories.isNotEmpty()) {
                categories.getOrNull(binding.spCategory.selectedItemPosition)?.id
            } else {
                null
            }

            val selectedPriorityId = if (!useAi && priorities.isNotEmpty()) {
                priorities.getOrNull(binding.spPriority.selectedItemPosition)?.id
            } else {
                null
            }

            val selectedTeamId = if (!useAi && teams.isNotEmpty()) {
                teams.getOrNull(binding.spTeam.selectedItemPosition)?.id
            } else {
                null
            }

            viewModel.createIncident(
                useAi = useAi,
                title = title,
                description = description,
                categoryId = selectedCategoryId,
                priorityId = selectedPriorityId,
                teamId = selectedTeamId
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.categories.collect { list ->
                        categories = list
                        binding.spCategory.adapter = createSpinnerAdapter(list.map { it.name })
                    }
                }

                launch {
                    viewModel.priorities.collect { list ->
                        priorities = list
                        binding.spPriority.adapter = createSpinnerAdapter(list.map { it.name })
                    }
                }

                launch {
                    viewModel.teams.collect { list ->
                        teams = list
                        binding.spTeam.adapter = createSpinnerAdapter(list.map { it.name })
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is CreateIncidentUiState.Idle -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnCreateIncident.isEnabled = true
                            }

                            is CreateIncidentUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.btnCreateIncident.isEnabled = false
                            }

                            is CreateIncidentUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnCreateIncident.isEnabled = true
                                Toast.makeText(
                                    this@CreateIncidentActivity,
                                    "Incidencia creada: ${state.referenceCode}",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }

                            is CreateIncidentUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnCreateIncident.isEnabled = true
                                Toast.makeText(
                                    this@CreateIncidentActivity,
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                viewModel.resetState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
    private fun setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollCreateIncident) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val keyboardBottom = (imeBottom - navBottom).coerceAtLeast(0)

            view.updatePadding(bottom = keyboardBottom)

            insets
        }
    }
}