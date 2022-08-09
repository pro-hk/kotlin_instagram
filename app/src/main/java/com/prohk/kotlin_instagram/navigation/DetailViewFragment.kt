package com.prohk.kotlin_instagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentDetailBinding
import com.prohk.kotlin_instagram.databinding.ItemDetailBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO

class DetailViewFragment : Fragment() {
    lateinit var binding: FragmentDetailBinding

    var firestore: FirebaseFirestore? = null

    var uid:String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uid = FirebaseAuth.getInstance().currentUser?.uid

        binding = FragmentDetailBinding.inflate(inflater,container,false)

        firestore = FirebaseFirestore.getInstance()
        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return binding.root
    }

    inner class CustomViewHolder(var binding:ItemDetailBinding) : RecyclerView.ViewHolder(binding.root)
    inner class DetailViewRecyclerViewAdapter() : RecyclerView.Adapter<CustomViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp",Query.Direction.DESCENDING)?.addSnapshotListener { value, error ->
                contentDTOs.clear()
                contentUidList.clear()
                // 로그아웃 시 안정성을 위해
                if(value == null) return@addSnapshotListener

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


        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val viewHolder = holder.binding

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
            //Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitemProfileImage)
            firestore?.collection("profileImages")
                ?.document(contentDTOs[position].uid!!)
                ?.addSnapshotListener { value, error ->
                    var url = value?.data!!["image"]
                    Glide.with(holder.itemView.context)
                        .load(url)
                        .into(viewHolder.detailviewitemProfileImage)
                }

            // 프로필 이미지 클릭했을 때
            viewHolder.detailviewitemProfileImage.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

            // 댓글이미지 눌렀을 때 창 전환
            viewHolder.detailviewitemCommentImageview.setOnClickListener {
                var intent = Intent(it.context, CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                startActivity(intent)
            }
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