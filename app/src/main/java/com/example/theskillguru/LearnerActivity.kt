package com.example.theskillguru

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LearnerActivity : AppCompatActivity() {

    private lateinit var btnNext: Button
    private lateinit var skillSet: ArrayList<String>
    private lateinit var options: Array<String>
    private val selectedItems = mutableSetOf<String>()
    private var popupWindow: PopupWindow? = null
    private var btnClicked: Boolean = false
    private var both: Boolean = false
    private lateinit var progressDialog: Dialog
    val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private val SPINNER_ID = R.id.Spinner_Id
        private val BACK_ICON_ID = R.id.backIcon_id
        private val OPTIONS_LIST_VIEW_ID = R.id.optionsListView
        private val FILTER_EDIT_TEXT_ID = R.id.filterEditText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_learner)
        setupEdgeToEdge()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize the progress dialog
        progressDialog = Dialog(this).apply {
            setContentView(R.layout.progressdialog)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }

        both = intent.getBooleanExtra("both", false)
        skillSet = if (both) {
            intent.getStringArrayListExtra("skillLearner") ?: arrayListOf()
        } else {
            intent.getStringArrayListExtra("skill") ?: arrayListOf()
        }
        options = skillSet.toTypedArray()

        setupUI()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        val backIcon: ImageView = findViewById(BACK_ICON_ID)
        val spinner: Spinner = findViewById(SPINNER_ID)
        btnNext = findViewById(R.id.btnNext)

        setupSpinner(spinner)
        setupButtonActions()

        backIcon.setOnClickListener { finish() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinner(spinner: Spinner) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setOnTouchListener { view, _ ->
            showPopupWindow(view)
            true
        }
    }

    private fun setupButtonActions() {
        btnNext.setOnClickListener {
            btnClicked = true
            updateSelectedItems()
            popupWindow?.dismiss()
        }
    }


    @SuppressLint("InflateParams")
    private fun showPopupWindow(anchor: View) {
        if (popupWindow == null) {
            val popupView = LayoutInflater.from(this).inflate(R.layout.popup_window_layout, null)
            val filterEditText = popupView.findViewById<EditText>(FILTER_EDIT_TEXT_ID)
            val listView = popupView.findViewById<ListView>(OPTIONS_LIST_VIEW_ID)
            val checkboxAdapter = DropdownAdapter(this, options, selectedItems)
            listView.adapter = checkboxAdapter

            filterEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    checkboxAdapter.filter(s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                setBackgroundDrawable(null)
                isFocusable = true
                isOutsideTouchable = false
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                setOnDismissListener {
                    btnClicked = false
                    updateSelectedItems()
                }
                showAsDropDown(anchor, 0, 0)
            }

            // Request focus for EditText and show keyboard
            filterEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(filterEditText, InputMethodManager.SHOW_IMPLICIT)
        } else {
            popupWindow?.showAsDropDown(anchor, 0, 0)
        }
    }

    private fun updateSelectedItems() {
        btnNext.isClickable = selectedItems.isNotEmpty()

        if (btnClicked && selectedItems.isNotEmpty()) {
            // Show the progress dialog before starting the update
            progressDialog.show()

            db.collection("Users").document(userId).update(hashMapOf("LearnerSkills" to selectedItems.toList()) as Map<String, Any>)
                .addOnSuccessListener {
                    // Dismiss progress dialog on success

                    progressDialog.dismiss()
                    if (both) {
                        val skillGuru = intent.getStringArrayListExtra("skillGuru") ?: arrayListOf()
                        val guruIntent = Intent(this, GuruActivity::class.java)
                        guruIntent.putExtra("skill", skillGuru)
                        startActivity(guruIntent)
                    }

                    else{

                        startActivity(Intent(this@LearnerActivity, MobileNumActivity::class.java))

                    }
                }
                .addOnFailureListener { e ->
                    // Dismiss progress dialog on failure
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to choose skills: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }


    }

}
