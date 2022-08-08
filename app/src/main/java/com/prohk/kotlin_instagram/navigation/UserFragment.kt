package com.prohk.kotlin_instagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.LoginActivity
import com.prohk.kotlin_instagram.MainActivity
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentUserBinding
import com.prohk.kotlin_instagram.databinding.ItemImageviewBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO

class UserFragment:Fragment() {
    lateinit var binding: FragmentUserBinding

    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null

    // 계정 판단
    var currentUserId: String? = null

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
        if(uid == currentUserId) {
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
        }


        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        return binding.root
    }
    inner class CustomViewHolder(val binding: ItemImageviewBinding) : RecyclerView.ViewHolder(binding.root)
    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<CustomViewHolder>() {

        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener{value, error ->
                // null일 경우 앱 안정성을 위한 return
                if(value == null) return@addSnapshotListener

                // get data
                for(snapshot in value.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                binding.accountTvPostCount.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ItemImageviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            imageView.imageviewIamge.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }



        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(holder.binding.imageviewIamge)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
    }
}