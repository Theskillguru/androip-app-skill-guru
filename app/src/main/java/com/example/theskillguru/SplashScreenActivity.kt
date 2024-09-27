package com.example.theskillguru

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Create a delay handler
        Handler(Looper.getMainLooper()).postDelayed({
            // After the delay, check user data
            checkUserData()
        }, 3100) // 3 second delay
    }

    private fun checkUserData() {
        // Only check if there's a current user
        if (currentUser != null) {
            db.collection("Users").document(currentUser).get().addOnSuccessListener {
                // Process the result
                if (it != null && it.data?.get("checkData") != null) {
                    if (it.data?.get("checkData") == true) {
                        navigateToActivity(QuestionActivity::class.java)
                    } else {
                        navigateToActivity(LoginActivity::class.java)
                    }
                } else {
                    navigateToActivity(LoginActivity::class.java)
                }
            }.addOnFailureListener {
                // Handle failure (e.g., no internet connection or Firebase error)
                navigateToActivity(LoginActivity::class.java)
            }
        } else {
            navigateToActivity(LoginActivity::class.java)
        }
    }

    private fun navigateToActivity(targetActivity: Class<*>) {
        // Navigate to the specified activity after the delay
        val intent = Intent(this@SplashScreenActivity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
