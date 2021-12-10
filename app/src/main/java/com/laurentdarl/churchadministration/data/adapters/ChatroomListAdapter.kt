package com.laurentdarl.churchadministration.data.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.laurentdarl.churchadministration.data.models.Chatroom
import com.nostra13.universalimageloader.core.ImageLoader
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.presentation.chat.ChatFragment
import java.lang.NullPointerException
import java.util.ArrayList

class ChatroomListAdapter(context: Context?, chatRoom: ArrayList<Chatroom>?) : ArrayAdapter<Chatroom>(
    context!!, 0, chatRoom!!
) {
    private val TAG = "ChatroomListAdapter"

    private var mLayoutResource = 0
    private var mContext: Context? = null
    private var mInflater: LayoutInflater? = null

    fun ChatroomListAdapter(
        @NonNull context: Context,
        @LayoutRes resource: Int,
        @NonNull objects: List<Chatroom?>?
    ) {
        mContext = context
        mLayoutResource = resource
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    class ViewHolder {
        var name: TextView? = null
        var creatorName: TextView? = null
        var numberMessages: TextView? = null
        var mProfileImage: ImageView? = null
        var mTrash: ImageView? = null
        var leaveChat: Button? = null
        var layoutContainer: RelativeLayout? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView = mInflater!!.inflate(mLayoutResource, parent, false)
            holder = ViewHolder()
            holder.name = convertView.findViewById<View>(R.id.name) as TextView
            holder.creatorName = convertView.findViewById<View>(R.id.creator_name) as TextView
            holder.numberMessages =
                convertView.findViewById<View>(R.id.number_chatmessages) as TextView
            holder.mProfileImage = convertView.findViewById<View>(R.id.profile_image) as ImageView
            holder.mTrash = convertView.findViewById<View>(R.id.icon_trash) as ImageView
            holder.leaveChat = convertView.findViewById<View>(R.id.leave_chat) as Button
            holder.layoutContainer =
                convertView.findViewById<View>(R.id.layout_container) as RelativeLayout
        } else {
            holder = convertView.tag as ViewHolder
        }
        val reference = FirebaseDatabase.getInstance().reference
        try {
            //set the chatroom name
            holder.name?.text  = getItem(position)?.chatroom_name

            //set the number of chat messages
            val chatMessagesString: String =
                getItem(position)?.chatroom_messages?.let {
                    java.lang.String.valueOf(it.size)
                        .toString()
                } + " messages"
            holder.numberMessages!!.text = chatMessagesString

            //get the users details who created the chatroom
            val query: Query = reference.child(mContext!!.getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(getItem(position)?.creator_id)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (singleSnapshot in dataSnapshot.children) {
                        Log.d(
                            TAG, "onDataChange: Found chat room creator: "
                                    + (singleSnapshot.getValue(User::class.java)?.name)
                        )
                        val createdBy =
                            "created by " + singleSnapshot.getValue(User::class.java)?.name
                        holder.creatorName!!.text = createdBy
                        ImageLoader.getInstance().displayImage(
                            singleSnapshot.getValue(User::class.java)?.profile_image,
                            holder.mProfileImage
                        )
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
            holder.mTrash!!.setOnClickListener {
                if (getItem(position)?.creator_id.equals(
                        FirebaseAuth.getInstance().currentUser!!.uid
                    )
                ) {
                    Log.d(TAG, "onClick: asking for permission to delete icon.")
                    (mContext as ChatFragment?)?.showDeleteChatroomDialog(getItem(position)?.chatroom_id)
                } else {
                    Toast.makeText(mContext, "You didn't create this chatroom", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            holder.layoutContainer!!.setOnClickListener {
                Log.d(TAG, "onClick: navigating to chatroom")
                getItem(position)?.let { it1 -> (mContext as ChatFragment?)?.joinChatroom(it1) }
            }
            /*
-------- Check if user is part of this chatroom --------
    1) if they are: give them ability to leave it
    2) if they aren't: hide the leave button
*/
            val usersInChatroom: List<String>? = getItem(position)?.users
            if (usersInChatroom!!.contains(FirebaseAuth.getInstance().currentUser!!.uid)) {
                holder.leaveChat!!.visibility = View.VISIBLE
                holder.leaveChat!!.setOnClickListener {
                    Log.d(
                        TAG,
                        "onClick: leaving chatroom with id: " + getItem(position)?.chatroom_id
                    )
                    val reference = FirebaseDatabase.getInstance().reference
                    getItem(position)?.chatroom_id?.let { it1 ->
                        reference.child(mContext!!.getString(R.string.dbnode_chatrooms))
                            .child(it1)
                            .child(mContext!!.getString(R.string.field_users))
                            .child(FirebaseAuth.getInstance().currentUser!!.uid)
                            .removeValue()
                    }
                    holder.leaveChat!!.visibility = View.GONE
                }
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "getView: NullPointerException: ", e.cause)
        }
        return super.getView(position, convertView, parent)
    }

}