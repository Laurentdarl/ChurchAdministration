package com.laurentdarl.churchadministration.presentation.chat

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.adapters.ChatroomListAdapter
import com.laurentdarl.churchadministration.data.models.ChatMessage
import com.laurentdarl.churchadministration.data.models.Chatroom
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.databinding.FragmentChatBinding
import com.laurentdarl.churchadministration.presentation.dialogs.DeleteChatroomDialog
import com.laurentdarl.churchadministration.presentation.dialogs.NewChatroomDialog
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ChatActivity"

    //widgets
    private var mListView: ListView? = null
    private var mFob: FloatingActionButton? = null

    //vars
    private var mChatrooms: ArrayList<Chatroom>? = null
    private var mAdapter: ChatroomListAdapter? = null
    private var mSecurityLevel = 0
    private val mChatroomReference: DatabaseReference? = null
    var isActivityRunning = false
    private var mNumChatroomMessages: HashMap<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(layoutInflater)
         mListView = binding.listView
        mFob = binding.fob

        init()

        // Inflate the layout for this fragment
        return binding.root
    }

    fun init() {
        mChatrooms = ArrayList()
        getUserSecurityLevel()
        mFob!!.setOnClickListener {
            val dialog = NewChatroomDialog()
//            dialog.show(requireContext(), getString(R.string.dialog_new_chatroom))
        }
    }

    private fun setupChatroomList() {
        mAdapter = ChatroomListAdapter(requireContext(), mChatrooms)
//        mListView!!.setAdapter(mAdapter)
    }

    /**
     * Join a chatroom selected by the user.
     * This method is executed from the ChatroomListAdapter class
     * This method checks to make sure the chatroom exists before joining.
     * @param chatroom
     */
    fun joinChatroom(chatroom: Chatroom) {
        //make sure the chatroom exists before joining
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey()
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot: DataSnapshot in dataSnapshot.children) {
                    val objectMap: Map<String, Any>? = singleSnapshot.value as HashMap<String, Any>?
                    if (objectMap!![getString(R.string.field_chatroom_id)].toString()
                        == chatroom.chatroom_id
                    ) {
                        if (mSecurityLevel >= chatroom.security_level!!.toInt()) {

                            //add user to the list of users who have joined the chatroom
                            addUserToChatroom(chatroom)

                            //navigate to the chatoom
//                            val intent = Intent(this@ChatActivity, ChatroomActivity::class.java)
//                            intent.putExtra(getString(R.string.intent_chatroom), chatroom)
//                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "insufficient security level",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        break
                    }
                }
                getChatrooms()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * add the current user to the list of users who have joined the chatroom.
     * Users who have joined the chatroom will receive notifications on chatroom activity.
     * They will receive notifications via a cloud functions sending a cloud message to the
     * chatroom ID (Sending via topic FCM)
     * @param chatroom
     */
    private fun addUserToChatroom(chatroom: Chatroom) {
        val reference = FirebaseDatabase.getInstance().reference
        chatroom.chatroom_id?.let {
            reference.child(getString(R.string.dbnode_chatrooms))
                .child(it)
                .child(getString(R.string.field_users))
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(getString(R.string.field_last_message_seen))
                .setValue(mNumChatroomMessages!![chatroom.chatroom_id])
        }
    }

    fun getChatrooms() {
        val reference = FirebaseDatabase.getInstance().reference
        mNumChatroomMessages = HashMap()
        if (mAdapter != null) {
            mChatrooms!!.clear()
        }
        val query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey()
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot: DataSnapshot in dataSnapshot.children) {
//                    Log.d(TAG, "onDataChange: found chatroom: "
//                            + singleSnapshot.getValue());
                    try {
                        if (singleSnapshot.exists()) {
                            val chatroom = Chatroom()
                            val objectMap: Map<String, Any>? =
                                singleSnapshot.value as HashMap<String, Any>?

                            chatroom.creator_id = objectMap?.get(getString(R.string.field_chatroom_id))
                                ?.toString()
                            chatroom.chatroom_name = objectMap?.get(getString(R.string.field_chatroom_name))
                                ?.toString()
                            chatroom.creator_id = objectMap?.get(getString(R.string.field_creator_id))
                                ?.toString()
                            chatroom.security_level = objectMap?.get(getString(R.string.field_security_level))
                                ?.toString()


                            //                    chatroom.setChatroom_id(singleSnapshot.getValue(Chatroom.class).getChatroom_id());
                            //                    chatroom.setSecurity_level(singleSnapshot.getValue(Chatroom.class).getSecurity_level());
                            //                    chatroom.setCreator_id(singleSnapshot.getValue(Chatroom.class).getCreator_id());
                            //                    chatroom.setChatroom_name(singleSnapshot.getValue(Chatroom.class).getChatroom_name());

                            //get the chatrooms messages
                            val messagesList: ArrayList<ChatMessage> = ArrayList<ChatMessage>()
                            var numMessages = 0
                            for (snapshot: DataSnapshot in singleSnapshot
                                .child(getString(R.string.field_chatroom_messages)).children) {
                                val message = ChatMessage()
                                message.timestamp = snapshot.getValue(ChatMessage::class.java)?.timestamp
                                message.user_id = snapshot.getValue(ChatMessage::class.java)?.user_id

                                message.message = snapshot.getValue(ChatMessage::class.java)?.message
                                messagesList.add(message)
                                numMessages++
                            }
                            if (messagesList.size > 0) {
                                chatroom.chatroom_messages = messagesList

                                //add the number of chatrooms messages to a hashmap for reference
                                mNumChatroomMessages!![chatroom.chatroom_id!!] = numMessages.toString()
                            }

                            //get the list of users who have joined the chatroom
                            val users: MutableList<String?> = ArrayList()
                            for (snapshot: DataSnapshot in singleSnapshot
                                .child(getString(R.string.field_users)).children) {
                                val user_id = snapshot.key
                                users.add(user_id)
                            }
                            if (users.size > 0) {
//                                chatroom.users = users
                            }
                            mChatrooms!!.add(chatroom)
                        }
                        setupChatroomList()
                    } catch (e: NullPointerException) {
                        Log.e(
                            "Chat Activity",
                            "onDataChange: NullPointerException: " + e.message
                        )
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getUserSecurityLevel() {
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_users))
            .orderByChild(getString(R.string.field_user_id))
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val singleSnapshot = dataSnapshot.children.iterator().next()
                val securityLevel: Int =
                    singleSnapshot.getValue(User::class.java)?.security_level!!.toInt()
                Log.d(
                    "Chat Activity",
                    "onDataChange: user has a security level of: $securityLevel"
                )
                mSecurityLevel = securityLevel
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun showDeleteChatroomDialog(chatroomId: String?) {
        val dialog = DeleteChatroomDialog()
        val args = Bundle()
        args.putString(getString(R.string.field_chatroom_id), chatroomId)
        dialog.arguments = args
//        dialog.show(requireActivity(), getString(R.string.dialog_delete_chatroom))
    }

    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
        getChatrooms()
    }

    override fun onStart() {
        super.onStart()
//        courses.pluralsight.com.tabianconsulting.ChatActivity.isActivityRunning = true
    }

    override fun onStop() {
        super.onStop()
//        courses.pluralsight.com.tabianconsulting.ChatActivity.isActivityRunning = false
    }

    private fun checkAuthenticationState() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
//            val intent = Intent(this@ChatActivity, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
        } else {

        }
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