package com.incidencias.ui.incident.tabs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.incidencias.R
import com.incidencias.data.remote.dto.attachment.AttachmentResponse
import com.incidencias.data.repository.AttachmentRepository
import com.incidencias.databinding.FragmentAttachmentsBinding
import com.incidencias.ui.incident.adapter.AttachmentAdapter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.incidencias.session.SessionManager
import kotlinx.coroutines.flow.first

class AttachmentsFragment : Fragment(R.layout.fragment_attachments) {

    companion object {
        private const val ARG_INCIDENT_ID = "arg_incident_id"
        private const val ARG_ATTACHMENTS = "arg_attachments"

        fun newInstance(
            incidentId: Long,
            attachments: List<AttachmentResponse>
        ): AttachmentsFragment {
            val fragment = AttachmentsFragment()
            val json = Gson().toJson(attachments)
            fragment.arguments = Bundle().apply {
                putLong(ARG_INCIDENT_ID, incidentId)
                putString(ARG_ATTACHMENTS, json)
            }
            return fragment
        }
    }

    private var _binding: FragmentAttachmentsBinding? = null
    private val binding get() = _binding!!

    private var incidentId: Long = -1L
    private var currentAttachments: MutableList<AttachmentResponse> = mutableListOf()
    private lateinit var adapter: AttachmentAdapter

    private var selectedUri: Uri? = null
    private var selectedFileName: String = "archivo"

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedUri = result.data?.data
                selectedFileName = getFileName(selectedUri) ?: "archivo"
                binding.tvSelectedFile.text = selectedFileName
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAttachmentsBinding.bind(view)

        incidentId = arguments?.getLong(ARG_INCIDENT_ID, -1L) ?: -1L

        val json = arguments?.getString(ARG_ATTACHMENTS).orEmpty()
        val type = object : TypeToken<List<AttachmentResponse>>() {}.type
        val items: List<AttachmentResponse> = Gson().fromJson(json, type) ?: emptyList()
        currentAttachments = items.toMutableList()

        adapter = AttachmentAdapter(currentAttachments) { attachment ->
            openDownloadUrl(attachment)
        }

        binding.recyclerViewAttachments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAttachments.adapter = adapter

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnUploadFile.setOnClickListener {
            uploadSelectedFile()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        pickFileLauncher.launch(intent)
    }

    private fun uploadSelectedFile() {
        val uri = selectedUri
        if (uri == null) {
            Toast.makeText(requireContext(), "Selecciona un archivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        if (incidentId == -1L) {
            Toast.makeText(requireContext(), "Incidencia no válida", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.progressBarAttachments.visibility = View.VISIBLE
            binding.btnUploadFile.isEnabled = false

            try {
                val file = copyUriToTempFile(uri, selectedFileName)
                val mimeType = requireContext().contentResolver.getType(uri) ?: "application/octet-stream"

                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val sessionManager = SessionManager(requireContext().applicationContext)
                val role = sessionManager.roleFlow.first().orEmpty().uppercase()

                val attachmentType = when (role) {
                    "TECHNICIAN" -> "TECHNICIAN_EVIDENCE"
                    "MANAGER" -> "MANAGER_EVIDENCE"
                    "ADMIN" -> "MANAGER_EVIDENCE"
                    else -> "USER_EVIDENCE"
                }

                val typeBody = attachmentType.toRequestBody("text/plain".toMediaTypeOrNull())

                val repository = AttachmentRepository(requireContext())
                val response = repository.uploadAttachment(incidentId, multipart, typeBody)

                binding.progressBarAttachments.visibility = View.GONE
                binding.btnUploadFile.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    currentAttachments.add(response.body()!!)
                    adapter.updateData(currentAttachments)
                    selectedUri = null
                    selectedFileName = "archivo"
                    binding.tvSelectedFile.text = "Ningún archivo seleccionado"
                    Toast.makeText(requireContext(), "Adjunto subido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No se pudo subir el archivo", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBarAttachments.visibility = View.GONE
                binding.btnUploadFile.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Error al subir archivo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openDownloadUrl(attachment: AttachmentResponse) {
        lifecycleScope.launch {
            try {
                val repository = AttachmentRepository(requireContext())
                val response = repository.getDownloadUrl(incidentId, attachment.id)

                if (response.isSuccessful && response.body() != null) {
                    val url = response.body()!!.url
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener la URL", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Error al abrir adjunto",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri, fileName: String): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo")

        val tempFile = File(requireContext().cacheDir, fileName)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        return tempFile
    }

    private fun getFileName(uri: Uri?): String? {
        if (uri == null) return null
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
