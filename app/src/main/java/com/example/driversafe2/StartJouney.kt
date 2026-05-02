package com.example.driversafe2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driversafe2.databinding.ActivityStartJouneyBinding

class StartJourneyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartJouneyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartJouneyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goButton.setOnClickListener {
            val from = binding.fromLocation.text.toString().trim()
            val to = binding.toLocation.text.toString().trim()
            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both From and To locations", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, JourneyMapActivity::class.java)
                intent.putExtra("from", from)
                intent.putExtra("to", to)
                startActivity(intent)
            }
        }
        binding.bac.setOnClickListener {
            startActivity(Intent(this,SelectionPage::class.java))
            finish()
        }
    }
}

