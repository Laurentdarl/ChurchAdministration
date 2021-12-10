package com.laurentdarl.churchadministration.presentation.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        FirebaseMessaging.getInstance().token.addOnSuccessListener(this) { instanceIdResult ->
            Log.e("newToken", instanceIdResult)
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.chat_app_container) as NavHostFragment
        val navController = navHost.navController
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp() = findNavController(R.id.chat_app_container).navigateUp()
}