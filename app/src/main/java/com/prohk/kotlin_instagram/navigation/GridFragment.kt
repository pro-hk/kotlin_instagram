package com.prohk.kotlin_instagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentGridBinding
import com.prohk.kotlin_instagram.databinding.ItemImageviewBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO

class GridFragment:Fragment() {

    var firestore: FirebaseFirestore? = null
    lateinit var binding: FragmentGridBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGridBinding.inflate(inflater, container, false)

        firestore = FirebaseFirestore.getInstance()

        binding.gridfragmentRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.gridfragmentRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        return binding.root
    }

    inner class CustomViewHolder(val binding: ItemImageviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<CustomViewHolder>() {

        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.addSnapshotListener { value, error ->
                    // null일 경우 앱 안정성을 위한 return
                    if (value == null) return@addSnapshotListener

                    // get data
                    for (snapshot in value.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
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