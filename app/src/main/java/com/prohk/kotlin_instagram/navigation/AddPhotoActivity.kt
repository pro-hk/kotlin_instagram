package com.prohk.kotlin_instagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.prohk.kotlin_instagram.databinding.ActivityAddPhotoBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    val binding by lazy { ActivityAddPhotoBinding.inflate(layoutInflater) }

    // 파일 업로드
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null

    // 파일 관리
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initiate storage(초기화)
        storage = FirebaseStorage.getInstance()

       // 파일 관리 초기화
       auth = FirebaseAuth.getInstance()
       firestore = FirebaseFirestore.getInstance()

        // 앨범 열기
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // 이미지 업로드 이벤트
        binding.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK) {
                // 사진을 선택했을 때 이미지 경로가 넘어옴
                photoUri = data?.data // 경로 담기
                binding.addphotoImage.setImageURI(photoUri) // 이미지에 경로 담기
            } else {
                // 취소버튼 눌렀을 때 작동
                finish()
            }
        }
    }

    fun contentUpload() {
        // 파일명 만들기
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        // 파일 생성 경로 설정
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        // 파일 업로드
        // 1. promise method
        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            // 다운로드 uri 입력
            contentDTO.imageUrl = uri.toString()

            // 유저 uid 입력
            contentDTO.uid = auth?.currentUser?.uid

            // 유저id 입력
            contentDTO.userId = auth?.currentUser?.email

            // 설명 입력
            contentDTO.explain = binding.addphotoEditExplain.text.toString()

            // 시간 입력
            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }


        // 2. callback method
        /*storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                // 다운로드 uri 입력
                contentDTO.imageUrl = uri.toString()

                // 유저 uid 입력
                contentDTO.uid = auth?.currentUser?.uid

                // 유저id 입력
                contentDTO.userId = auth?.currentUser?.email

                // 설명 입력
                contentDTO.explain = binding.addphotoEditExplain.toString()

                // 시간 입력
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }
        }*/
    }
}