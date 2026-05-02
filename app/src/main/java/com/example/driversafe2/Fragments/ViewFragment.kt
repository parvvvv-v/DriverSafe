package com.example.driversafe2.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driversafe2.*
import com.example.driversafe2.databinding.FragmentViewBinding
import com.google.firebase.database.*

class ViewFragment : Fragment() {
    private lateinit var binding: FragmentViewBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()
    private lateinit var username: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewBinding.inflate(inflater, container, false)

        val prefs = requireActivity().getSharedPreferences("DriverSafePrefs", android.content.Context.MODE_PRIVATE)
        val tempUsername: String? = prefs.getString("username", null)
        if (tempUsername == null) {
            Toast.makeText(requireContext(), "Error: User not found", Toast.LENGTH_LONG).show()
            requireActivity().finish()
            return binding.root
        }
        username = tempUsername
        databaseReference = FirebaseDatabase.getInstance().reference
        retrivemenuitems(username)
        binding.bac.setOnClickListener {
            startActivity(Intent(requireContext(), SelectionPage::class.java))
            requireActivity().finish()
        }
        return binding.root
    }

    private fun retrivemenuitems(username: String) {
        database = FirebaseDatabase.getInstance()
        val docref: DatabaseReference = database.reference
            .child("drivers")
            .child(username)
            .child("Documents")
        docref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (docsnapshot in snapshot.children) {
                    val docitems = docsnapshot.getValue(AllMenu::class.java)
                    val key = docsnapshot.key
                    if (docitems != null) {
                        docitems.key = key
                        menuItems.add(docitems)
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Error:${error.message}")
            }
        })
    }

    private fun setAdapter() {
        val adapter = ViewListAdapter(requireContext(), databaseReference, menuItems) { position ->
            deletemenuitems(position)
        }
        binding.viewdocumentsrecyclerview.layoutManager = LinearLayoutManager(requireContext())
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
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                menuItems.removeAt(position)
                binding.viewdocumentsrecyclerview.adapter?.notifyItemRemoved(position)
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Key not found", Toast.LENGTH_SHORT).show()
        }
    }
}
