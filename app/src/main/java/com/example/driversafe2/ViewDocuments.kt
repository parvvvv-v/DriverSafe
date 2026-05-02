package com.example.driversafe2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driversafe2.databinding.ActivityViewDocumentsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ViewDocuments : AppCompatActivity() {
    private lateinit var binding: ActivityViewDocumentsBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems:ArrayList<AllMenu> = ArrayList()
    private lateinit var username: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)
        val tempUsername: String? = prefs.getString("username", null)
        if (tempUsername == null) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        username = tempUsername
        databaseReference = FirebaseDatabase.getInstance().reference
        retrivemenuitems(username)
        binding.bac.setOnClickListener {
            startActivity(Intent(this,SelectionPage::class.java))
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun retrivemenuitems(username:String) {
        database = FirebaseDatabase.getInstance()
        val docref: DatabaseReference = database.reference
            .child("drivers")
            .child(username)
            .child("Documents")
        docref.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (docsnapshot in snapshot.children) {
                    val docitems = docsnapshot.getValue(AllMenu::class.java)
                    val key = docsnapshot.key
                    if (docitems != null) {
                        docitems.key = key  // <-- Add this line
                        menuItems.add(docitems)
                    }
                }

                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError","Error:${error.message}")
            }

        })
    }
    private fun setAdapter() {
        val adapter = ViewListAdapter(this@ViewDocuments,databaseReference,menuItems) {position->
            deletemenuitems(position)
        }
        binding.viewdocumentsrecyclerview.layoutManager = LinearLayoutManager(this)
        binding.viewdocumentsrecyclerview.adapter = adapter
    }

    private fun deletemenuitems(position: Int) {
        val menuitemtodelete = menuItems[position]
        val menuitemkey = menuitemtodelete.key

        if (menuitemkey != null) {
            val docItemReference = database.reference
                .child("drivers")
                .child(username)
                .child("Documents")
                .child(menuitemkey)
            docItemReference.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                menuItems.removeAt(position)
                binding.viewdocumentsrecyclerview.adapter?.notifyItemRemoved(position)
            }.addOnFailureListener {
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Key not found", Toast.LENGTH_SHORT).show()
        }
    }

}