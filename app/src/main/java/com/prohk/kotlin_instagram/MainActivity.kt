package com.prohk.kotlin_instagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.prohk.kotlin_instagram.databinding.ActivityMainBinding
import com.prohk.kotlin_instagram.navigation.*
import com.prohk.kotlin_instagram.navigation.util.FcmPush

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)

        // 사진 경로를 가져올 수 있는 권한 요청
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        // 메인화면이 뜨면 detail view가 default
        binding.bottomNavigation.selectedItemId = R.id.action_home

        // 로그인되자마자 push토큰 저장
        registerPushToken()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when (item.itemId) {
            R.id.action_home -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.action_search -> {
                var girdFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, girdFragment)
                    .commit()
                return true
            }
            R.id.action_add_photo -> { // firebase의 Storage 사용 - Rules 에서 if 부분 true 변경해야됨
                // 외부저장소 권한 체크
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment)
                    .commit()
                return true
            }
            R.id.action_account -> {
                var userFragment = UserFragment()
                var bundle = Bundle()
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid",uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }

    // firebase 알림 토큰 받기
    fun registerPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = task.result
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String, Any>()
            map["pushToken"] = token

            FirebaseFirestore.getInstance().collection("pushTokens").document(uid!!).set(map)
        }
    }

    fun setToolbarDefault() {
        binding.toolbarUsername.visibility = View.GONE
        binding.toolbarBtnBack.visibility = View.GONE
        binding.toolbarTitleImage.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == RESULT_OK) {
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!).continueWithTask {
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }
}