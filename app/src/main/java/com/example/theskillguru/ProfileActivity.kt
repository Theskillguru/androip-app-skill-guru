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

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnNext: Button

    companion object {
        private val SPINNER_ID = R.id.Spinner_Id
        private val BACK_ICON_ID = R.id.backIcon_id
        private val OPTIONS_LIST_VIEW_ID = R.id.optionsListView
        private val FILTER_EDIT_TEXT_ID = R.id.filterEditText
    }

    private var btnClicked: Boolean = false
    private val dbSkills = FirebaseFirestore.getInstance()
    private val options = arrayOf("Choose Your Profile", "Guru", "Learner")
    private val selectedItems = mutableSetOf<String>()
    private var popupWindow: PopupWindow? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val progress = Dialog(this)
        progress.setContentView(R.layout.progressdialog)
        progress.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progress.setCancelable(false)

        val backIcon: ImageView = findViewById(BACK_ICON_ID)
        val spinner: Spinner = findViewById(SPINNER_ID)
        btnNext = findViewById(R.id.btnNext)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setOnTouchListener { v, _ ->
            showPopupWindow(v)
            true
        }

        btnNext.setOnClickListener {
            btnClicked = true
            progress.show()
            updateSelectedItems {
                progress.dismiss()
            }
            popupWindow?.dismiss() // Close the popup when the button is clicked
        }

        backIcon.setOnClickListener {
            finish()
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

            popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                setBackgroundDrawable(null)
                isFocusable = true
                isOutsideTouchable = false
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED // Ensure the keyboard appears
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

    private fun updateSelectedItems(onComplete: (() -> Unit)? = null) {

        val name = intent.getStringExtra("name").toString().uppercase()
        val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val selectedItemsText = selectedItems.joinToString(", ")
        if (selectedItemsText.isNotEmpty()) {
            btnNext.isClickable = true
            if (btnClicked) {
                val arr: ArrayList<String> = arrayListOf()

                if (selectedItems.contains("Learner") && !selectedItems.contains("Guru")) {
                    dbSkills.collection("Users").document(userId)
                        .set(hashMapOf("Name" to name , "Profile" to selectedItems.elementAt(0))).addOnFailureListener{

                            Toast.makeText(this@ProfileActivity,"Something went wrong",Toast.LENGTH_LONG).show()
                        }

                    learnerSkill(arr) { isSuccess ->
                        if (isSuccess) {
                            val skillIntent = Intent(this@ProfileActivity, LearnerActivity::class.java)
                            skillIntent.putExtra("both", false)
                            skillIntent.putExtra("skill", arr)
                            startActivity(skillIntent)
                        }
                        onComplete?.invoke()
                    }
                } else if (selectedItems.contains("Guru") && !selectedItems.contains("Learner")) {

                    dbSkills.collection("Users").document(userId)
                        .set(hashMapOf("Name" to name , "Profile" to selectedItems.elementAt(0))).addOnFailureListener{

                            Toast.makeText(this@ProfileActivity,"Something went wrong",Toast.LENGTH_LONG).show()
                        }

                    guruSkill(arr) { isSuccess ->
                        if (isSuccess) {
                            val intent = Intent(this@ProfileActivity, GuruActivity::class.java)
                            intent.putExtra("skill", arr)
                            startActivity(intent)
                        }
                        onComplete?.invoke()
                    }
                } else if (selectedItems.contains("Guru") && selectedItems.contains("Learner")) {
                    dbSkills.collection("Users").document(userId)
                        .set(hashMapOf("Name" to name , "Profile" to "Both")).addOnFailureListener{

                            Toast.makeText(this@ProfileActivity,"Something went wrong",Toast.LENGTH_LONG).show()
                        }
                    val guruArr: ArrayList<String> = arrayListOf()
                    learnerSkill(arr) { learnerSuccess ->
                        if (learnerSuccess) {
                            guruSkill(guruArr) { guruSuccess ->
                                if (guruSuccess) {
                                    val skillIntent = Intent(this@ProfileActivity, LearnerActivity::class.java)
                                    skillIntent.putExtra("both", true)
                                    skillIntent.putExtra("skillLearner", arr)
                                    skillIntent.putExtra("skillGuru", guruArr)
                                    startActivity(skillIntent)
                                }
                                onComplete?.invoke()
                            }
                        } else {
                            onComplete?.invoke()
                        }
                    }
                }
            }
        } else {
            onComplete?.invoke()
        }
    }

    private fun learnerSkill(arr: ArrayList<String>, onComplete: (Boolean) -> Unit) {
        arr.add("Choose Skill For Learner")
        dbSkills.collection("theSkillGuru").document("Learner").get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val skillList = it.get("skills") as? ArrayList<String>
                    if (skillList != null) {
                        arr.addAll(skillList)
                        onComplete(true) // Data is ready, proceed to the next step
                    } else {
                        Toast.makeText(this@ProfileActivity, "Skills are empty", Toast.LENGTH_LONG).show()
                        onComplete(false)
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Skills not found", Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this@ProfileActivity, "Failed to fetch skills", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
    }

    private fun guruSkill(arr: ArrayList<String>, onComplete: (Boolean) -> Unit) {
        arr.add("Choose Skill For Guru")
        dbSkills.collection("theSkillGuru").document("Guru").get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val skillList = it.get("skills") as? ArrayList<String>
                    if (skillList != null) {
                        arr.addAll(skillList)
                        onComplete(true) // Data is ready, proceed to the next step
                    } else {
                        Toast.makeText(this@ProfileActivity, "Skills are empty", Toast.LENGTH_LONG).show()
                        onComplete(false)
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Skills not found", Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this@ProfileActivity, "Failed to fetch skills", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
    }
}
