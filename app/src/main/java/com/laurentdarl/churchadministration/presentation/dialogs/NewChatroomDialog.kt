package com.laurentdarl.churchadministration.presentation.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.ChatMessage
import com.laurentdarl.churchadministration.data.models.Chatroom
import com.laurentdarl.churchadministration.presentation.chat.ChatFragment
import java.text.SimpleDateFormat
import java.util.*

class NewChatroomDialog: DialogFragment()  {
    private val TAG = "NewChatroomDialog"

    private var mSeekBar: SeekBar? = null
    private var mChatroomName: EditText? = null
    private var mCreateChatroom: TextView? = null
    private  var mSecurityLevel:TextView? = null
    private var mUserSecurityLevel = 0
    private var mSeekProgress = 0


    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.dialog_new_chatroom, container, false)
        mChatroomName = view.findViewById<View>(R.id.input_chatroom_name) as EditText
        mSeekBar = view.findViewById<View>(R.id.input_security_level) as SeekBar
        mCreateChatroom = view.findViewById<View>(R.id.create_chatroom) as TextView
        mSecurityLevel = view.findViewById<View>(R.id.security_level) as TextView
        mSeekProgress = 0
        mSecurityLevel!!.text = mSeekProgress.toString()
        getUserSecurityLevel()
        mCreateChatroom!!.setOnClickListener {
            if (mChatroomName!!.text.toString() != "") {
                Log.d(TAG, "onClick: creating new chat room")
                if (mUserSecurityLevel >= mSeekBar!!.progress) {
                    val reference = FirebaseDatabase.getInstance().reference
                    //get the new chatroom unique id
                    val chatroomId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().key

                    //create the chatroom
                    val chatroom = Chatroom()
                    chatroom.security_level =  mSeekBar!!.progress.toString()
                    chatroom.chatroom_name = mChatroomName!!.text.toString()
                    chatroom.creator_id = FirebaseAuth.getInstance().currentUser!!.uid
                    chatroom.creator_id = chatroomId


                    //insert the new chatroom into the database
                    reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId!!)
                        .setValue(chatroom)

                    //create a unique id for the message
                    val messageId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().key

                    //insert the first message into the chatroom
                    val message = ChatMessage()
                    message.message = "Welcome to the new chatroom!"
                    message.timestamp = getTimestamp()
                    reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId)
                        .child(getString(R.string.field_chatroom_messages))
                        .child(messageId!!)
                        .setValue(message)
                    (activity as ChatFragment?)?.getChatrooms()
                    dialog!!.dismiss()
                } else {
                    Toast.makeText(activity, "insuffient security level", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                mSeekProgress = i
                mSecurityLevel!!.text = mSeekProgress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        return view
    }

    private fun getUserSecurityLevel() {
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_users))
            .orderByKey() //OR could use ->.orderByChild(getString(R.string.field_user_id))
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //alternatively could have used:
                //DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                for (singleSnapshot in dataSnapshot.children) {
                    Log.d(
                        TAG, "onDataChange: users security level: "
                                + (singleSnapshot.getValue(User::class.java)?.security_level)
                    )
                    mUserSecurityLevel = java.lang.String.valueOf(
                        singleSnapshot.getValue(User::class.java)?.security_level
                    ).toInt()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Return the current timestamp in the form of a string
     * @return
     */
    private fun getTimestamp(): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }

}