package com.example.driversafe2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driversafe2.databinding.ActivityHistoryBinding
import com.google.firebase.database.*

data class AlertEvent(val date: String? = null, val time: String? = null)

class History : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var alertRef: DatabaseReference
    private lateinit var adapter: AlertAdapter
    private val alertList = mutableListOf<AlertEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        val prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)
        val username = prefs.getString("username", null)

        if (username == null) {
            Toast.makeText(this, "No driver info found!", Toast.LENGTH_SHORT).show()
            return
        }
        alertRef = database.getReference("drivers").child(username).child("alerts")

        adapter = AlertAdapter(alertList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadAlerts()
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
                Toast.makeText(this@History, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        binding.bac.setOnClickListener {
            startActivity(Intent(this,SelectionPage::class.java))
            finish()
        }
    }

}
