package com.example.driversafe2.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driversafe2.AlertAdapter
import com.example.driversafe2.AlertEvent
import com.example.driversafe2.databinding.FragmentHistoryBinding
import com.google.firebase.database.*

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var alertRef: DatabaseReference
    private lateinit var adapter: AlertAdapter
    private val alertList = mutableListOf<AlertEvent>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance()

        val prefs = requireContext().getSharedPreferences("DriverSafePrefs", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)

        if (username == null) {
            Toast.makeText(requireContext(), "No driver info found!", Toast.LENGTH_SHORT).show()
            return
        }

        alertRef = database.getReference("drivers").child(username).child("alerts")

        adapter = AlertAdapter(alertList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadAlerts()

        binding.bac.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadAlerts() {
        alertRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alertList.clear()
                for (data in snapshot.children) {
                    val event = data.getValue(AlertEvent::class.java)
                    if (event != null) alertList.add(event)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
