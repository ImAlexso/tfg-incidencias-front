package com.incidencias.ui.incident.tabs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.incidencias.R
import com.incidencias.data.remote.dto.message.IncidentMessageResponse
import com.incidencias.data.repository.MessageRepository
import com.incidencias.databinding.FragmentInternalMessagesBinding
import com.incidencias.ui.incident.adapter.InternalMessageAdapter
import kotlinx.coroutines.launch

class InternalMessagesFragment : Fragment(R.layout.fragment_internal_messages) {

    companion object {
        private const val ARG_INCIDENT_ID = "arg_incident_id"
        private const val ARG_MESSAGES = "arg_messages"

        fun newInstance(
            incidentId: Long,
            messages: List<IncidentMessageResponse>
        ): InternalMessagesFragment {
            val fragment = InternalMessagesFragment()
            val json = Gson().toJson(messages.filter { it.visibility == "INTERNAL" })
            fragment.arguments = Bundle().apply {
                putLong(ARG_INCIDENT_ID, incidentId)
                putString(ARG_MESSAGES, json)
            }
            return fragment
        }
    }

    private var _binding: FragmentInternalMessagesBinding? = null
    private val binding get() = _binding!!

    private var incidentId: Long = -1L
    private var currentMessages: MutableList<IncidentMessageResponse> = mutableListOf()
    private lateinit var adapter: InternalMessageAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInternalMessagesBinding.bind(view)

        incidentId = arguments?.getLong(ARG_INCIDENT_ID, -1L) ?: -1L

        val json = arguments?.getString(ARG_MESSAGES).orEmpty()
        val type = object : TypeToken<List<IncidentMessageResponse>>() {}.type
        val items: List<IncidentMessageResponse> = Gson().fromJson(json, type) ?: emptyList()
        currentMessages = items.toMutableList()

        adapter = InternalMessageAdapter(currentMessages)
        binding.recyclerViewInternalMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewInternalMessages.adapter = adapter

        binding.btnSendInternalMessage.setOnClickListener {
            sendInternalMessage()
        }
    }

    private fun sendInternalMessage() {
        val text = binding.etNewInternalMessage.text?.toString()?.trim().orEmpty()

        if (text.isBlank()) {
            Toast.makeText(requireContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.progressBarInternalMessages.visibility = View.VISIBLE
            binding.btnSendInternalMessage.isEnabled = false

            try {
                val repository = MessageRepository(requireContext())
                val response = repository.addInternalMessage(incidentId, text)

                binding.progressBarInternalMessages.visibility = View.GONE
                binding.btnSendInternalMessage.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    currentMessages.add(response.body()!!)
                    adapter.updateData(currentMessages)
                    binding.etNewInternalMessage.setText("")
                    binding.recyclerViewInternalMessages.scrollToPosition(currentMessages.lastIndex)
                    Toast.makeText(requireContext(), "Mensaje interno enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No se pudo enviar el mensaje", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBarInternalMessages.visibility = View.GONE
                binding.btnSendInternalMessage.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Error al enviar mensaje interno",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}