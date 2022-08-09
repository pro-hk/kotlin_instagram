package com.prohk.kotlin_instagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.LoginActivity
import com.prohk.kotlin_instagram.MainActivity
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentUserBinding
import com.prohk.kotlin_instagram.databinding.ItemImageviewBinding
import com.prohk.kotlin_instagram.navigation.model.AlarmDTO
import com.prohk.kotlin_instagram.navigation.model.ContentDTO
import com.prohk.kotlin_instagram.navigation.model.FollowDTO

class UserFragment : Fragment() {
    lateinit var binding: FragmentUserBinding

    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null

    // 계정 판단
    var currentUserId: String? = null

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        var fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        binding = FragmentUserBinding.inflate(inflater, container, false)

        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        currentUserId = auth?.currentUser?.uid
        if (uid == currentUserId) {
            // My Page
            binding.accountBtnFollowSignout.text = getString(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // Other User Page
            binding.accountBtnFollowSignout.text = getString(R.string.follow)
            var mainBinding = activity as MainActivity
            mainBinding.binding.toolbarUsername.text = arguments?.getString("userId")
            mainBinding.binding.toolbarBtnBack.setOnClickListener {
                mainBinding.binding.bottomNavigation.selectedItemId = R.id.action_home
            }
            mainBinding.binding.toolbarTitleImage.visibility = View.GONE
            mainBinding.binding.toolbarUsername.visibility = View.VISIBLE
            mainBinding.binding.toolbarBtnBack.visibility = View.VISIBLE
            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }
        }

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        // 프로필 사진 올리기
        binding.accountIvProfile.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return binding.root
    }

    // 프로필 이미지 변경
    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { value, error ->
                if (value == null) return@addSnapshotListener
                if (value?.data != null) {
                    var url = value?.data!!["image"]
                    Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop())
                        .into(binding.accountIvProfile)
                }
            }
    }

    // 팔로우
    fun requestFollow() {
        // 나의 계정에는 누구를 팔로우 하는지
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserId!!)
        firestore?.runTransaction {
            var followDTO = it.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true // 중복 following 방지 위한 상대방 uid 입력

                it.set(tsDocFollowing, followDTO!!)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                // 팔로잉 취소
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)
            } else {
                // 팔로잉
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            it.set(tsDocFollowing, followDTO!!)
            return@runTransaction
        }
        // 상대방 계정에는 또 다른 타인이 팔로우 하는지
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction {
            var followDTO = it.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserId!!] = true

                it.set(tsDocFollower, followDTO!!)

                followAlarm(uid!!)
                return@runTransaction
            }

            if (followDTO!!.followers.containsKey(currentUserId)) {
                // 상대방 계정에 팔로우 했을 경우
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserId!!)
            } else {
                // 상대방 계정에 팔로우 하지 않은 경우
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserId!!] = true
                followAlarm(uid!!)
            }
            it.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if (value == null) return@addSnapshotListener
            var followDTO = value.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount != null) {
                binding.accountTvFollowingCount.text = followDTO?.followingCount.toString()
            }
            if (followDTO?.followerCount != null) {
                binding.accountTvFollowerCount.text = followDTO.followerCount.toString()

                if(uid == currentUserId) return@addSnapshotListener

                if (followDTO.followers.containsKey(currentUserId!!)) {
                    binding.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                    binding.accountBtnFollowSignout.background.setColorFilter(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.colorLightGray
                        ), PorterDuff.Mode.MULTIPLY
                    )
                } else {
                    binding.accountBtnFollowSignout.text = getString(R.string.follow)
                    binding.accountBtnFollowSignout.background.colorFilter = null
                }
            }
        }
    }

    // 팔로우 알람
    fun followAlarm(destinationUid: String) {
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class CustomViewHolder(val binding: ItemImageviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<CustomViewHolder>() {

        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { value, error ->
                    // null일 경우 앱 안정성을 위한 return
                    if (value == null) return@addSnapshotListener

                    // get data
                    for (snapshot in value.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    binding.accountTvPostCount.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageView =
                ItemImageviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            imageView.imageviewIamge.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }


        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(holder.binding.imageviewIamge)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
    }
}