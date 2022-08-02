package com.prohk.kotlin_instagram.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentDetailBinding
import com.prohk.kotlin_instagram.databinding.ItemDetailBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO

class DetailViewFragment : Fragment() {
    val binding by lazy { FragmentDetailBinding.inflate(layoutInflater) }

    var firestore: FirebaseFirestore? = null

    var uid:String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uid = FirebaseAuth.getInstance().currentUser?.uid

        var binding = FragmentDetailBinding.inflate(inflater,container,false)

        firestore = FirebaseFirestore.getInstance()
        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return binding.root
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<DetailViewRecyclerViewAdapter.CustomViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { value, error ->
                contentDTOs.clear()
                contentUidList.clear()
                for(snapshot in value!!.documents) {
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var view = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(binding:ItemDetailBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val viewHolder = ItemDetailBinding.inflate(layoutInflater)

            // userId
            viewHolder.detailviewitemProfileTextview.text = contentDTOs[position].userId

            // image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitemImageviewContent)

            // explain
            viewHolder.detailviewitemExplainTextview.text = contentDTOs[position].explain

            // like
            viewHolder.detailviewitemFavoritecounterTextview.text = "Likes " + contentDTOs[position].favoriteCount
            
            // 버튼 클릭 이벤트
            viewHolder.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(position)
            }
            if(contentDTOs!![position].favorites.containsKey(uid)) {
                viewHolder.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            } else {
                viewHolder.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // profile image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitemProfileImage)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        fun favoriteEvent(position: Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                if(contentDTO!!.favorites.containsKey(uid)) {
                    // 버튼이 클릭되어 있을 때
                    contentDTO?.favoriteCount = contentDTO!!.favoriteCount - 1
                    contentDTO?.favorites?.remove(uid)
                } else {
                    // 버튼이 클릭되어 있지 않을 때
                    contentDTO?.favoriteCount = contentDTO!!.favoriteCount + 1
                    contentDTO!!.favorites[uid!!] = true
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
    }
}