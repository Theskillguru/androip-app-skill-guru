@file:Suppress("DEPRECATION")

package com.example.theskillguru

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {
    private lateinit var nameET: EditText
    private lateinit var passET: EditText
    private lateinit var emailET: EditText
    private lateinit var btnSignUp: Button
    private lateinit var googleIcon: ImageView
    private lateinit var googleSignInClient: GoogleSignInClient
    private var passwordVisible: Boolean = false
    private lateinit var mAuth: FirebaseAuth
    private lateinit var forgotPass :TextView
    private var txtName = ""
    private var txtEmail = ""
    private var txtPass = ""
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        progressDialog = Dialog(this).apply {
            setContentView(R.layout.progressdialog)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }
        // Initialize views
        nameET = findViewById(R.id.name_edittext)
        passET = findViewById(R.id.password_edittext)
        emailET = findViewById(R.id.Email_edittext)
        googleIcon = findViewById(R.id.ivGoogle)
        btnSignUp = findViewById(R.id.btnSignUp)
        forgotPass = findViewById(R.id.passForgot)

        settoggle()

        mAuth = Firebase.auth

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnSignUp.setOnClickListener {
            signInEmailPass()
        }

        googleIcon.setOnClickListener {
            signInGoogle()
        }
        forgotPass.setOnClickListener{ passRest()}
    }

    private fun passRest() {
        txtEmail = emailET.text.toString()

        mAuth.sendPasswordResetEmail(txtEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                    forgotPass.visibility = TextView.GONE
                } else {
                    Toast.makeText(this, "Unable to send reset email", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.e("LoginActivity", "Google Sign-In failed.")
            Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Google sign in failed: ${e.statusCode}")
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        showProgressDialog()
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideProgressDialog()
                if (task.isSuccessful) {
                    val user = mAuth.currentUser

                    isAvailable { isAvailable ->
                        if (isAvailable) {
                            val intent = Intent(this@LoginActivity, QuestionActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        } else {
                            updateUi(user)
                        }
                    }

                } else {
                    Log.e("LoginActivity", "Firebase Authentication failed: ${task.exception?.message}")
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInEmailPass() {
        txtName = nameET.text.toString()
        txtEmail = emailET.text.toString()
        txtPass = passET.text.toString()

        if (txtEmail.isEmpty()) {
            emailET.error = "Required"
        }
        if (txtName.isEmpty()){
            nameET.error = "Required"
        }
        if (txtPass.isEmpty() || txtPass.length < 6) {
            passET.error = "Required"
        } else {
            showProgressDialog()
            mAuth.signInWithEmailAndPassword(txtEmail, txtPass)
                .addOnCompleteListener { signInTask ->
                    hideProgressDialog()
                    if (signInTask.isSuccessful) {
                        Toast.makeText(this@LoginActivity, "Sign in Successful", Toast.LENGTH_SHORT).show()
                        isAvailable { isAvailable ->
                            if (isAvailable) {
                                val intent = Intent(this@LoginActivity, QuestionActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            } else {
                                updateUi(mAuth.currentUser)
                            }
                        }
                    } else {
                        val exception = signInTask.exception
                        Log.d("error",exception.toString())
                        if (exception is FirebaseAuthUserCollisionException) {

                            Toast.makeText(this@LoginActivity,"Incorrect Email or Password",Toast.LENGTH_SHORT).show()

                        } else {
                            mAuth.createUserWithEmailAndPassword(txtEmail, txtPass)
                                .addOnCompleteListener { signUpTask ->
                                    hideProgressDialog()
                                    if (signUpTask.isSuccessful) {
                                        Toast.makeText(this@LoginActivity, "Login in Successful", Toast.LENGTH_SHORT).show()
                                        updateUi(mAuth.currentUser)
                                    } else {
                                        if (exception is FirebaseAuthInvalidCredentialsException){
                                            forgotPass.visibility = TextView.VISIBLE
                                        }

                                        Toast.makeText(this@LoginActivity, "${signUpTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                }
        }
    }

    private fun updateUi(user: FirebaseUser?) {
        user?.let {
            // Generate and store FCM token
            generateAndStoreFCMToken(it.uid)

            val intent = Intent(this@LoginActivity, ProfileActivity::class.java)
            intent.putExtra("name", it.displayName ?: txtName)
            startActivity(intent)
        }
    }

    private fun generateAndStoreFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d(TAG, "FCM Token: $token")
            Toast.makeText(baseContext, "FCM token generated", Toast.LENGTH_SHORT).show()

            // Store token in Firestore
            storeFCMTokenInFirestore(userId, token)
        }
    }

    private fun storeFCMTokenInFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(userId)

        userRef.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM Token successfully stored in Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error storing FCM token in Firestore", e)
            }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }

    private fun isAvailable(callback: (Boolean) -> Unit): Boolean {
        val userId = mAuth.currentUser?.uid.toString()
        val db = FirebaseFirestore.getInstance()
        var check = false
        db.collection("Users").document(userId).get().addOnCompleteListener { task ->

            if (task.isSuccessful) {
                val checkData = task.result.data?.get("checkData") == true
                callback(checkData)
                check = checkData// Use the callback to return the result
            } else {
                callback(false) // Return false if the task was not successful
            }
        }
        return check
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun settoggle() {
        passET.setOnTouchListener { _, event ->
            val right = 2
            if (event?.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= passET.right - passET.compoundDrawables[right].bounds.width() - 80) {
                    val selection: Int = passET.selectionEnd
                    if (passwordVisible) {
                        passET.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hidepasstoggle, 0)
                        passET.transformationMethod = PasswordTransformationMethod.getInstance()
                        passwordVisible = false
                    } else {
                        passET.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.showpass, 0)
                        passET.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        passwordVisible = true
                    }
                    passET.setSelection(selection)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun showProgressDialog() {
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}


//    private fun updateUi(user: FirebaseUser?) {
//        user?.let {
//            val userId = it.uid
//            val userName = it.displayName ?: txtName
//
//            updateUserDataAndNavigate(userId, userName)
//        }
//    }

//    private fun updateUserDataAndNavigate(userId: String, userName: String) {
//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//                navigateToProfileActivity(userName)
//                return@addOnCompleteListener
//            }
//
//            val token = task.result
//
//            val userData = hashMapOf(
//                "name" to userName,
//                "fcmToken" to token
//            )
//
//            FirebaseFirestore.getInstance().collection("Users").document(userId)
//                .set(userData, SetOptions.merge())
//                .addOnSuccessListener {
//                    Log.d(TAG, "User data and FCM token saved to Firestore")
//                    navigateToProfileActivity(userName)
//                }
//                .addOnFailureListener { e ->
//                    Log.w(TAG, "Error saving user data and FCM token to Firestore", e)
//                    navigateToProfileActivity(userName)
//                }
//        }
//    }


//    private fun sendFCMTokenToServer() {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        if (currentUser != null) {
//            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val token = task.result
//                    val userId = currentUser.uid
//                    FirebaseFirestore.getInstance().collection("Users").document(userId)
//                        .update("fcmToken", token)
//                        .addOnSuccessListener {
//                            Log.d(TAG, "FCM token updated in Firestore")
//                        }
//                        .addOnFailureListener { e ->
//                            Log.w(TAG, "Error updating FCM token in Firestore", e)
//                        }
//                } else {
//                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//                }
//            }
//        }
//    }