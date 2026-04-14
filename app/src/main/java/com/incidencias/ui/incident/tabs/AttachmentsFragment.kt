package com.incidencias.ui.incident.tabs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.incidencias.R
import com.incidencias.databinding.FragmentAttachmentsBinding
import com.incidencias.ui.incident.IncidentDetailViewModel
import com.incidencias.ui.incident.adapter.AttachmentAdapter
import kotlinx.coroutines.launch

class AttachmentsFragment : Fragment(R.layout.fragment_attachments) {

    private var _binding: FragmentAttachmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentDetailViewModel by activityViewModels()
    private lateinit var adapter: AttachmentAdapter

    private var selectedUri: Uri? = null
    private var selectedFileName: String = "archivo"

    private val pickFileLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedUri = result.data?.data
                selectedFileName = getFileName(selectedUri) ?: "archivo"
                binding.tvSelectedFile.text = selectedFileName
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAttachmentsBinding.bind(view)

        adapter = AttachmentAdapter(emptyList()) { attachment ->
            viewModel.openAttachment(attachment.id)
        }

        binding.recyclerViewAttachments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAttachments.adapter = adapter

        binding.btnSelectFile.setOnClickListener { openFilePicker() }

        binding.btnUploadFile.setOnClickListener {
            val uri = selectedUri ?: return@setOnClickListener
            viewModel.uploadAttachment(uri, selectedFileName)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val detail = state.detail ?: return@collect
                    val isClosed = detail.statusName.equals("CLOSED", ignoreCase = true)

                    adapter.updateData(detail.attachments)

                    binding.progressBarAttachments.isVisible = state.isUploadingAttachment

                    binding.layoutUploadControls.visibility = if (isClosed) View.GONE else View.VISIBLE
                    binding.btnSelectFile.isEnabled = !state.isUploadingAttachment
                    binding.btnUploadFile.isEnabled = !state.isUploadingAttachment

                    if (!state.isUploadingAttachment) {
                        selectedUri = null
                        selectedFileName = "archivo"
                        binding.tvSelectedFile.text = "Ningún archivo seleccionado"
                    }
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        pickFileLauncher.launch(intent)
    }

    private fun getFileName(uri: Uri?): String? {
        if (uri == null) return null

        val resolver = requireContext().contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}