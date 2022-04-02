package com.twoweeks.dogJalal

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

const val CREATE_FILE = 1

class ListAdapter (val context : Context, val userList : ArrayList<UserModel>) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_item, null)

//        val photo = view.findViewById<ImageView>(R.id.photo)
        val nickname = view.findViewById<TextView>(R.id.show_nickname)
        val hashtag = view.findViewById<TextView>(R.id.show_hashtag)
        val profile = view.findViewById<ImageView>(R.id.show_picture)


        val user = userList[position]
//        val resourceId = context.resources.getIdentifier(user.photo, "drawable", context.packageName)
//        photo.setImageResource(resourceId)
        nickname.text = user.nickname
        hashtag.text = user.hashtag.toString().replace(",", "")
            .replace("[", "").replace("]","")

//        val conImage : Uri = Uri.parse(user.profileUrl)

        //profile.setImageURI(conImage)

        return view
    }

    override fun getItem(position: Int): Any {
        return userList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return userList.size
    }

    fun returnList() : ArrayList<UserModel>{
        return userList
    }

}