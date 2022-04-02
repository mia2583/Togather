package com.twoweeks.dogJalal

import android.content.ContentProvider
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.AdapterView
import androidx.appcompat.app.ActionBar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
//import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
//import com.google.protobuf.Value
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_profile.*


class MainActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference

    var UList : ArrayList<UserModel> = ArrayList<UserModel>()

    fun changeList(value:UserModel){
        UList.add(value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Theme_Togather)
        // if not logined -> activity_login
        // else -> activity_main
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Firebase.database.reference

        database.child("user").get().addOnSuccessListener {
            if(it.value != null){
                val temp = it.value as HashMap<String, HashMap<String, HashMap<String, String>>>
                for (key in temp!!.keys) {
                    val getValues = temp.get(key) as HashMap<String, String>
                    val tuid = getValues.get("uid")!!
                    val tname = getValues.get("nickname")!!
                    val ttag: ArrayList<String> = getValues!!.get("hashtag") as ArrayList<String>
                    //val tpuid = getValues.get("profileUrl")!!
                    val tempUser = UserModel(tuid, tname, ttag)
                    changeList(tempUser)
                }
                val userAdapter = ListAdapter(applicationContext, UList)
                search_lv.adapter = userAdapter
            }
        }

        profile_btn.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        search_btn.setOnClickListener{
            var searchList = ArrayList<UserModel>()
            val findTag = search_et.getText().toString()

            if(findTag == ""){
                val totalAdapter = ListAdapter(applicationContext, UList)
                search_lv.adapter = totalAdapter
                return@setOnClickListener
            }
            else {
                for (i in 0..UList.size - 1) {
                    val oneUser = UList.get(i)
                    val userHashtag = oneUser.hashtag
                    for(str in userHashtag){
                        if (str.contains(findTag)) {
                            searchList.add(oneUser)
                            break
                        }
                    }
                }
                val searchAdapter = ListAdapter(applicationContext, searchList)
                search_lv.adapter = searchAdapter
            }

        }

        search_lv.onItemClickListener = AdapterView.OnItemClickListener{parent, view, position, id ->
            val selection = parent.getItemAtPosition(position) as UserModel

            Log.i("uid get?", selection.uid)
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiver", selection.uid)
            intent.putExtra("nickName", selection.nickname)
            startActivity(intent)
        }
        addUserListener(database)
    }

    private fun addUserListener(userReference : DatabaseReference){
        var usersList = ArrayList<UserModel>()
        val userListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if(dataSnapshot.getValue() == null) return

                val temp = dataSnapshot.getValue() as HashMap<String, HashMap<String, HashMap<String, String>>>
                val gotUser = temp.get("user")

                for (key in gotUser!!.keys){
                    val getValues = gotUser!!.get(key)
                    val tuid = getValues!!.get("uid")
                    val tname = getValues!!.get("nickname")
                    val ttag : ArrayList<String> = getValues!!.get("hashtag") as ArrayList<String>
                    // val tpuid = getValues!!.get("profileUrl")
                    val tempUser = UserModel(tuid!!, tname!!, ttag)

                    if (!usersList.contains(tempUser)){
                        usersList.add(tempUser!!)
                    }
                    val userAdapter = ListAdapter(applicationContext, usersList)
                    search_lv.adapter = userAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("userAdd", "Error")
            }
        }
        userReference.addValueEventListener(userListener)
    }
}