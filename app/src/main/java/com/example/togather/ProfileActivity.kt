package com.example.togather

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ActionMenuView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProfileActivity : AppCompatActivity() {

    lateinit var database: FirebaseDatabase
    lateinit var databaseReference: DatabaseReference

    lateinit var profileUri : Uri

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Authentication, Database, Storage
        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference()

        profile_iv.setOnClickListener(View.OnClickListener() {
            when{
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                ->{
                    navigateGallery()
                }

                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ->{
                    showPermissionContextPopup()
                }
                else -> requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1000
                )
            }

        })


        var hashtags = ArrayList<String>()

        hashtag_add_btn.setOnClickListener(View.OnClickListener(){
            hashtags.add(hashtag_et.getText().toString())
            hashtag_et.setText(null)

            hashtag_show.setText(hashtags.joinToString(separator = " "))
        })


        profile_okay_btn.setOnClickListener(View.OnClickListener() {
            addProfile("", name_et.getText().toString(), hashtags)
            startActivity(Intent(this, MainActivity::class.java))
        })

    }

    private fun addProfile(profileImageUrl:String, nickname: String, hashtags: ArrayList<String>) {
        val user = Firebase.auth.currentUser
        if(user != null){
            if(hashtags.isEmpty()) hashtags.add(" ")
            val uid = user.uid
            val newUser = UserModel(uid, nickname, hashtags)
            databaseReference.child("user").child(uid).setValue(newUser)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            1000 -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    navigateGallery()
                else
                    Toast.makeText(this, "Denied Permission", Toast.LENGTH_SHORT).show()
            }
            else ->{}
        }
    }

    private fun navigateGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when(requestCode){
            2000 -> {
                val selectedImageUri : Uri? = data?.data
                if(selectedImageUri != null){
                    profile_iv.setImageURI(selectedImageUri)
                    profileUri = selectedImageUri
                }
                else{
                    Toast.makeText(this, "Cannot load Image", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Cannot load Image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionContextPopup(){
        AlertDialog.Builder(this)
            .setTitle("Needs Permission")
            .setMessage("Needs Gallery Access Permission")
            .setPositiveButton("Agree"){_,_ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1000)
            }
            .setNegativeButton("Disagree"){_,_ ->}
            .create()
            .show()
    }
}
