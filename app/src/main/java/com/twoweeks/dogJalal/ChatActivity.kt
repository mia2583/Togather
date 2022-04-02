package com.twoweeks.dogJalal

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.chat_left.*

class ChatActivity : AppCompatActivity() {
    //recyclerView 관련 변수
    private lateinit var adapter: ChatAdapter
    var datas: ArrayList<Chat> = arrayListOf()

    // 로그인 정보 관련 변수
    private lateinit var auth: FirebaseAuth
    lateinit var currentUser: FirebaseUser
    lateinit var sender: String
    lateinit var receiver: String
    private lateinit var nickName: String

    // 파이어베이스 연결 관련 변수
    lateinit var database: FirebaseDatabase
    lateinit var myRef: DatabaseReference

    // 갤러리 권한 관련 변수
    val REQUEST_GALLERY_TAKE = 2
    lateinit var storage: FirebaseStorage
    lateinit var photoUrl: Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        // 데이터베이스 연결
        database = Firebase.database
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 현재 사용자의 정보 가져오기
        auth = Firebase.auth
        currentUser = auth.currentUser!!
        sender = currentUser.uid
        // 채팅 상재 정보 가져오기
        receiver = intent.getStringExtra("receiver") ?: ""
        if (receiver == "") finish()
        nickName = intent.getStringExtra("nickName") ?: "(닉네임 없음)"
        tv_nickname.text = nickName
        // 리사이클러뷰 연결
        adapter = ChatAdapter(datas, sender)
        rc_chat.adapter = adapter
        rc_chat.setHasFixedSize(true)
        rc_chat.layoutManager = LinearLayoutManager(this)
        // 이전 채팅 내역 가져오기
        readChatList()
        //파이어베이스 스토리지 연결
        storage = FirebaseStorage.getInstance()

        myRef = database.getReference("message")
        // 실시간 메세지 받기
        receiveMessage()

        // 메세지 전송하기
        btn_send.setOnClickListener {
            // 날짜 데이터 받아오기
            val c: Calendar = Calendar.getInstance()
            val dataformat: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_hhmmss")
            var datetime: String = dataformat.format(c.getTime())
            sendMessage("0", "", datetime)
        }

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)


        btn_sendImg.setOnClickListener {
            loadImage()
        }
    }

    // 실시간 메세지 전송 함수
    fun sendMessage(type: String, path: String, datetime: String) {
        var inputMsg: String = et_message.getText().toString()
        if(type == "1") { inputMsg = path }
        //Log.e("이미지?", path)
        // 아무것도 입력 안하면 보내기 버튼 비활성화
        if (inputMsg.isNotEmpty()) {
            // 시간별로 날짜가 쌓임
            myRef = database.getReference("message").child(receiver).child(sender).child(datetime)

            // 보내는 형식에 메세지 담기
            val sendMsg: HashMap<String, String> = hashMapOf()
            sendMsg.put("img", type)
            sendMsg.put("msg", inputMsg)

            // 데이터베이스에 저장
            myRef.setValue(sendMsg)
                .addOnSuccessListener {
                    // 채팅 내용 파일에 저장
                    var fos: FileOutputStream = openFileOutput(receiver, MODE_APPEND)
                    fos.write("${type}\n".toByteArray())
                    fos.write("${sender}\n".toByteArray())
                    fos.write("${inputMsg}\n".toByteArray())
                    fos.write("${datetime}\n".toByteArray())
                    fos.close()
                    // 채팅 보이기
                    adapter.addChat(Chat(type, sender, inputMsg, datetime))
                    rc_chat.scrollToPosition(adapter.itemCount - 1)
                    // 메세지 입력 초기화
                    et_message.setText("")
                    inputMsg = ""

                }
                .addOnFailureListener {
                    Toast.makeText(this, "전송에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 이전 채팅 내역 불러오기 함수
    fun readChatList() {
        try {
            // 파일명이 receiver인 파일 열기
            var fis: FileInputStream = openFileInput(receiver)
            var buffer: BufferedReader = BufferedReader(InputStreamReader(fis))
            var line: String? = buffer.readLine(); // 파일에서 한줄을 읽어옴
            while (line != null) {
                var type: String? = line
                line = buffer.readLine()
                var from: String? = line
                line = buffer.readLine()
                var msg: String? = line
                line = buffer.readLine()
                var time: String? = line
                adapter.addChat(Chat(type?:"0", from ?: "", msg ?: "", time ?: ""))
                line = buffer.readLine()
            }
        } catch (e: FileNotFoundException) {
            //Toast.makeText(this, "채팅 내역이 없습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "채팅 불러오기 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 실시간 메세지 받기 함수
    fun receiveMessage() {
        myRef = database.getReference("message").child(sender).child(receiver)
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // 채팅 내용 읽기
                for (d in dataSnapshot.children) {
                    var time = d.key.toString()
                    Log.e("chatData", d.toString())
                    var type = d.child("img").value.toString()
                    var msg = d.child("msg").value.toString()

                    // 채팅 내용 파일에 저장
                    var fos: FileOutputStream = openFileOutput(receiver, MODE_APPEND)
                    fos.write("${type}\n".toByteArray())
                    fos.write("${receiver}\n".toByteArray())
                    fos.write("${msg}\n".toByteArray())
                    fos.write("${time}\n".toByteArray())
                    fos.close()

                    adapter.addChat(Chat(type, receiver, msg, time))
                }
                myRef.removeValue()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("snap1", "수신 실패")
            }
        })
    }

    private fun loadImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    private fun uploadImage() {
        //파일 이름 생성
        var timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format((Date()))
        var imageFileName = "IMAGE" + timestamp + ".png"
        var storageRef = storage?.reference?.child("sendImages")?.child(imageFileName)

        // 이미지 파이어베이스에 저장하기
        val uploadTask: StorageTask<*>
        uploadTask = storageRef.putFile(photoUrl!!)
        uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if(!task.isSuccessful) { task.exception?.let { throw it } }
            return@Continuation storageRef.downloadUrl
        }).addOnCompleteListener { task ->
            // 성공시 이미지 url 보내기
            if (task.isSuccessful) {
                val downloadUrl = task.result
                val url = downloadUrl.toString()
                sendMessage("1", url, timestamp)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==REQUEST_GALLERY_TAKE) {
            if(resultCode == Activity.RESULT_OK) {
                // 선택한 이미지 경로
                photoUrl = data?.data!!

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    uploadImage()
                } else {
                    //취소 시 작동
                    //finish()
                }
            }
        }
    }
}
