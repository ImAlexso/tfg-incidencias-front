package com.incidencias.ui.incident.tabs

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.incidencias.R
import com.incidencias.data.remote.dto.incident.IncidentEventResponse

class HistoryFragment : Fragment(R.layout.fragment_simple_list) {

    companion object {
        private const val ARG_EVENTS = "arg_events"

        fun newInstance(events: List<IncidentEventResponse>): HistoryFragment {
            val fragment = HistoryFragment()
            val json = Gson().toJson(events)
            fragment.arguments = Bundle().apply {
                putString(ARG_EVENTS, json)
            }
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listView = view.findViewById<ListView>(R.id.listView)
        val json = arguments?.getString(ARG_EVENTS).orEmpty()

        val type = object : TypeToken<List<IncidentEventResponse>>() {}.type
        val items: List<IncidentEventResponse> = Gson().fromJson(json, type) ?: emptyList()

        val texts = items.map { "${it.eventType}\n${it.eventDescription}\n${it.createdAt}" }
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, texts)
    }
}