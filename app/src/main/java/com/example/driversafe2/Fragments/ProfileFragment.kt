package com.example.driversafe2.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.driversafe2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    private lateinit var adminreference: DatabaseReference
    private lateinit var username: String
    private var selectedGender = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        adminreference = database.reference.child("drivers")
        binding.phonenumber.isEnabled = false
        binding.profilename.isEnabled = false
        binding.vehiclenumber.isEnabled = false
        binding.dobentry.isEnabled = false
        binding.genderSpinner.isEnabled = false
        var isEnabled = false
        binding.editbutton.setOnClickListener {
            isEnabled = !isEnabled
            binding.phonenumber.isEnabled = isEnabled
            binding.profilename.isEnabled = isEnabled
            binding.vehiclenumber.isEnabled = isEnabled
            binding.dobentry.isEnabled = isEnabled
            binding.genderSpinner.isEnabled = isEnabled
            if (isEnabled) {
                binding.editbutton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                binding.editbutton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                binding.editbutton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                binding.editbutton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
        }
        val genders = arrayOf("Select Gender", "Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        binding.genderSpinner.adapter = adapter
        binding.genderSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedGender = if (position != 0) genders[position] else ""
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })
        binding.dobentry.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
                    binding.dobentry.setText(formattedDate)
                },
                year, month, day
            )
            datePicker.show()
        }
        val prefs = requireContext().getSharedPreferences("DriverSafePrefs", android.content.Context.MODE_PRIVATE)
        val tempUsername = prefs.getString("username", null)
        if (tempUsername == null) {
            Toast.makeText(requireContext(), "Error: User not found", Toast.LENGTH_LONG).show()
            return
        }
        username = tempUsername
        userRef = database.reference.child("drivers").child(username)
        loadUserProfile()

        binding.saveinfobutton.setOnClickListener {
            updateUserProfile()
        }
        binding.bac.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun loadUserProfile() {
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").getValue(String::class.java)
                val vehicle = snapshot.child("vehivle").getValue(String::class.java)
                val phone = snapshot.child("phonenumber").getValue(String::class.java)
                val gender = snapshot.child("gender").getValue(String::class.java)
                val dob = snapshot.child("dob").getValue(String::class.java)
                binding.profilename.setText(name)
                binding.vehiclenumber.setText(vehicle)
                binding.phonenumber.setText(phone)
                binding.dobentry.setText(dob)
                val genders = arrayOf("Select Gender", "Male", "Female", "Other")
                val index = genders.indexOf(gender ?: "")
                if (index >= 0) binding.genderSpinner.setSelection(index)
                else binding.genderSpinner.setSelection(0)
            } else {
                Toast.makeText(requireContext(), "Profile data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateUserProfile() {
        val updatednumber = binding.phonenumber.text.toString().trim()
        val updateprofile = binding.profilename.text.toString().trim()
        val updatedvehiclenumber = binding.vehiclenumber.text.toString().trim()
        val updateddob = binding.dobentry.text.toString().trim()
        val updatedgender = selectedGender
        if (updateprofile.isEmpty() || updatednumber.isEmpty() || updateddob.isEmpty() || updatedgender.isEmpty()) {
            Toast.makeText(requireContext(), "Fields Cannot Be Left Empty", Toast.LENGTH_SHORT).show()
            return
        }
        val updates = mapOf(
            "name" to updateprofile,
            "vehivle" to updatedvehiclenumber,
            "phonenumber" to updatednumber,
            "gender" to updatedgender,
            "dob" to updateddob
        )
        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                binding.editbutton.text = "Edit"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Update Failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }
}
