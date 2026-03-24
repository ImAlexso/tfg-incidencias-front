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
import com.incidencias.databinding.FragmentPublicMessagesBinding
import com.incidencias.ui.incident.adapter.PublicMessageAdapter
import kotlinx.coroutines.launch

class PublicMessagesFragment : Fragment(R.layout.fragment_public_messages) {

    companion object {
        private const val ARG_INCIDENT_ID = "arg_incident_id"
        private const val ARG_MESSAGES = "arg_messages"

        fun newInstance(
            incidentId: Long,
            messages: List<IncidentMessageResponse>
        ): PublicMessagesFragment {
            val fragment = PublicMessagesFragment()
            val json = Gson().toJson(messages.filter { it.visibility != "INTERNAL" })
            fragment.arguments = Bundle().apply {
                putLong(ARG_INCIDENT_ID, incidentId)
                putString(ARG_MESSAGES, json)
            }
            return fragment
        }
    }

    private var _binding: FragmentPublicMessagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PublicMessageAdapter
    private var incidentId: Long = -1L
    private var currentMessages: MutableList<IncidentMessageResponse> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPublicMessagesBinding.bind(view)

        incidentId = arguments?.getLong(ARG_INCIDENT_ID, -1L) ?: -1L

        val json = arguments?.getString(ARG_MESSAGES).orEmpty()
        val type = object : TypeToken<List<IncidentMessageResponse>>() {}.type
        val items: List<IncidentMessageResponse> = Gson().fromJson(json, type) ?: emptyList()
        currentMessages = items.toMutableList()

        adapter = PublicMessageAdapter(currentMessages)
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMessages.adapter = adapter

        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val text = binding.etNewMessage.text?.toString()?.trim().orEmpty()

        if (text.isBlank()) {
            Toast.makeText(requireContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        if (incidentId == -1L) {
            Toast.makeText(requireContext(), "Incidencia no válida", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.progressBarMessages.visibility = View.VISIBLE
            binding.btnSendMessage.isEnabled = false

            try {
                val repository = MessageRepository(requireContext())
                val response = repository.addPublicMessage(incidentId, text)

                binding.progressBarMessages.visibility = View.GONE
                binding.btnSendMessage.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    currentMessages.add(response.body()!!)
                    adapter.updateData(currentMessages)
                    binding.etNewMessage.setText("")
                    binding.recyclerViewMessages.scrollToPosition(currentMessages.lastIndex)
                    Toast.makeText(requireContext(), "Mensaje enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No se pudo enviar el mensaje", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBarMessages.visibility = View.GONE
                binding.btnSendMessage.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Error al enviar el mensaje",
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