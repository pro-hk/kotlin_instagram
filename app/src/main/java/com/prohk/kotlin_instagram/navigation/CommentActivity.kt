package com.prohk.kotlin_instagram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.databinding.ActivityCommentBinding
import com.prohk.kotlin_instagram.databinding.ItemCommentBinding
import com.prohk.kotlin_instagram.navigation.model.ContentDTO

class CommentActivity : AppCompatActivity() {

    val binding by lazy { ActivityCommentBinding.inflate(layoutInflater) }

    var contentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        contentUid = intent.getStringExtra("contentUid")

        binding.commentRecyclerview.adapter = CommentRecyclerviewAdapter()
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        binding.commentBtnSend.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .document()
                .set(comment)

            binding.commentEditMessage.setText("")
        }
    }

    inner class CustomViewHolder(val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root)
    inner class CommentRecyclerviewAdapter: RecyclerView.Adapter<CustomViewHolder>() {
        var comments: ArrayList<ContentDTO.Comment> = arrayListOf()
        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { value, error ->
                    comments.clear()
                    if(value == null) return@addSnapshotListener

                    for(snapshot in value!!) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var view = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            var view = holder.binding
            view.commentviewitemTextviewComment.text = comments[position].comment
            view.commentviewitemTextviewProfile.text = comments[position].userId
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener {
                    if(it.isSuccessful) {
                        var url = it.result!!["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(view.commentviewitemImageviewProfile)
                    }
                }
        }

        override fun getItemCount(): Int {
            return comments.size
        }
    }
}