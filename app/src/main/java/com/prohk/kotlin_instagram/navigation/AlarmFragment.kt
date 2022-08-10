package com.prohk.kotlin_instagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prohk.kotlin_instagram.R
import com.prohk.kotlin_instagram.databinding.FragmentAlarmBinding
import com.prohk.kotlin_instagram.databinding.ItemCommentBinding
import com.prohk.kotlin_instagram.navigation.model.AlarmDTO

class AlarmFragment : Fragment() {
    val binding by lazy { FragmentAlarmBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter()
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return binding.root
    }

    inner class CustomViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class AlarmRecyclerviewAdapter() : RecyclerView.Adapter<CustomViewHolder>() {
        var alarmDTOList: ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid)
                .addSnapshotListener { value, error ->
                    alarmDTOList.clear()
                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            var view =
                ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            FirebaseFirestore.getInstance().collection("profileImages")
                .document(alarmDTOList[position].uid!!).get().addOnCompleteListener {
                    if(it.isSuccessful) {
                        val url = it.result["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(holder.binding.commentviewitemImageviewProfile)
                    }
            }

            when (alarmDTOList[position].kind) {
                0 -> {
                    val str_0 = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    holder.binding.commentviewitemTextviewProfile.text = str_0
                }
                1 -> {
                    val str_0 =
                        alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + " of " + alarmDTOList[position].message
                    holder.binding.commentviewitemTextviewProfile.text = str_0
                }
                2 -> {
                    val str_0 =
                        alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    holder.binding.commentviewitemTextviewProfile.text = str_0
                }
            }
            holder.binding.commentviewitemTextviewComment.visibility = View.GONE
        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

    }
}