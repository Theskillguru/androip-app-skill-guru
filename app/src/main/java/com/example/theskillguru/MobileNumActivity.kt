package com.example.theskillguru

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hbb20.CountryCodePicker

class MobileNumActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var ccp: CountryCodePicker
    private lateinit var phoneET: EditText
    private lateinit var btnNext: Button
    lateinit var backIcon :ImageView
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mobile_num)

        // Initialize progress dialog
        progressDialog = Dialog(this).apply {
            setContentView(R.layout.progressdialog)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }

        // Set up window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        ccp = findViewById(R.id.idCcp)
        phoneET = findViewById(R.id.phoneNum)
        btnNext = findViewById(R.id.btnNext)
        backIcon = findViewById(R.id.backIcon_id)
        ccp.registerCarrierNumberEditText(phoneET)

        backIcon.setOnClickListener{ finish()}

        btnNext.setOnClickListener {
            val phoneNumber = phoneET.text.toString()
            if (phoneNumber.length != 10) {
                phoneET.error = "Enter valid Number"
            } else {
                // Show progress dialog
                progressDialog.show()

                // Update Firestore in a single call
                val userUpdates = mapOf(
                    "Mobile" to ccp.fullNumber,
                    "toggleOnline" to false,
                    "checkData" to true
                )

                db.collection("Users").document(userId).update(userUpdates)
                    .addOnSuccessListener {
                        // Dismiss progress dialog and navigate to the next activity
                        progressDialog.dismiss()

                        val intent = Intent(this@MobileNumActivity,QuestionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()  // Optionally finish this activity
                    }
                    .addOnFailureListener { e ->
                        // Dismiss progress dialog and show error message
                        progressDialog.dismiss()
                        Toast.makeText(this, "unexpected error : ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}
