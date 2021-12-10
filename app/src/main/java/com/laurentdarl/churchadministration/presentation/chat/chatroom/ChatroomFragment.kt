package com.laurentdarl.churchadministration.presentation.chat.chatroom

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.*
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.adapters.ChatMessageListAdapter
import com.laurentdarl.churchadministration.data.models.ChatMessage
import com.laurentdarl.churchadministration.data.models.Chatroom
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.databinding.FragmentChatroomBinding
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*

class ChatroomFragment : Fragment() {

    private var _binding: FragmentChatroomBinding? = null
    private val binding get() = _binding!!

    private val TAG = "ChatroomFragment"

    //firebase
    private var mAuthListener: AuthStateListener? = null
    private var mMessagesReference: DatabaseReference? = null

    //widgets
    private var mChatroomName: TextView? = null
    private var mListView: ListView? = null
    private var mMessage: EditText? = null
    private var mCheckmark: ImageView? = null

    //vars
    private var mChatroom: Chatroom? = null
    private var mMessagesList: List<ChatMessage>? = null
    private var mMessageIdSet: Set<String>? = null
    private var mAdapter: ChatMessageListAdapter? = null
    var isActivityRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatroomBinding.inflate(layoutInflater)

        mChatroomName = binding.textChatroomName
        mListView = binding.listView
        mMessage = binding.inputMessage
        mCheckmark = binding.checkmark
//        SupportActionBar().hide()

        setupFirebaseAuth()
        getChatroom()
        init()
        hideSoftKeyboard()

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun init() {
        mMessage!!.setOnClickListener {
//            mListView!!.setSelection(mAdapter.Count() - 1) //scroll to the bottom of the list
        }
        mCheckmark!!.setOnClickListener {
            if (mMessage!!.text.toString() != "") {
                val message = mMessage!!.text.toString()

                //create the new message object for inserting
                val newMessage = ChatMessage()
                newMessage.message = message
                newMessage.timestamp = getTimestamp()
                newMessage.user_id = FirebaseAuth.getInstance().currentUser!!.uid

                //get a database reference
                val reference = mChatroom?.chatroom_id?.let { it1 ->
                    FirebaseDatabase.getInstance().reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(it1)
                        .child(getString(R.string.field_chatroom_messages))
                }

                //create the new messages id
                val newMessageId = reference?.push()?.key

                //insert the new message into the chatroom
                reference?.child(newMessageId!!)?.setValue(newMessage)

                //clear the EditText
                mMessage!!.setText("")

                //refresh the messages list? Or is it done by the listener??
            }
        }
    }

    /**
     * Retrieve the chatroom name using a query
     */
    private fun getChatroom() {
//        val intent: Intent = getIntent()
//        if (intent.hasExtra(getString(R.string.intent_chatroom))) {
//            val chatroom: Chatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom))
//            Log.d(
//                courses.pluralsight.com.tabianconsulting.ChatroomActivity.TAG,
//                "getChatroom: chatroom: $chatroom"
//            )
//            mChatroom = chatroom
//            mChatroomName.setText(mChatroom.getChatroom_name())
//        }
            enableChatroomListener()
    }


    private fun getChatroomMessages() {
        if (mMessagesList == null) {
            mMessagesList = ArrayList()
            mMessageIdSet = HashSet()
            initMessagesList()
        }
        val reference = FirebaseDatabase.getInstance().reference
        val query: Query = reference.child(getString(R.string.dbnode_chatrooms))
            .child(mChatroom!!.chatroom_id!!)
            .child(getString(R.string.field_chatroom_messages))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    try { //need to catch null pointer here because the initial welcome message to the
                        //chatroom has no user id
                        val message = ChatMessage()
                        val userId: String = snapshot.getValue(ChatMessage::class.java)?.user_id!!

                        //check to see if the message has already been added to the list
                        //if the message has already been added we don't need to add it again
                        if (!mMessageIdSet!!.contains(snapshot.key!!)) {
                            //add the message id to the message set
//                            mMessageIdSet.add(snapshot.key)
                            if (userId != null) { //check and make sure it's not the first message (has no user id)
                                message.message = snapshot.getValue(ChatMessage::class.java)!!.message

                                message.user_id = snapshot.getValue(ChatMessage::class.java)!!.user_id

                                message.timestamp = snapshot.getValue(ChatMessage::class.java)!!.timestamp
                                message.profile_image = ""
                                message.name = ""
//                                mMessagesList.add(message)
                            } else {
                                message.message =
                                    snapshot.getValue(ChatMessage::class.java)!!.message
                                message.timestamp = snapshot.getValue(ChatMessage::class.java)!!.timestamp
                                message.profile_image = ""
                                message.name = ""
//                                mMessagesList.add(message)
                            }
                        }
                    } catch (e: NullPointerException) {
                        Log.e(
                            "ChatRoom Activity",
                            "onDataChange: NullPointerException: " + e.message
                        )
                    }
                }
                //query the users node to get the profile images and names
                getUserDetails()
//                mAdapter.notifyDataSetChanged() //notify the adapter that the dataset has changed
//                mListView!!.setSelection(mAdapter.getCount() - 1) //scroll to the bottom of the list
//                //initMessagesList();
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getUserDetails() {
        val reference = FirebaseDatabase.getInstance().reference
        for (i in mMessagesList!!.indices) {
            // Log.d(TAG, "onDataChange: searching for userId: " + mMessagesList.get(i).getUser_id());
            if (mMessagesList!![i].user_id != null && mMessagesList!![i].profile_image
                    .equals("")
            ) {
                val query: Query = reference.child(getString(R.string.dbnode_users))
                    .orderByKey()
                    .equalTo(mMessagesList!![i].user_id)
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val singleSnapshot = dataSnapshot.children.iterator().next()
                        mMessagesList!![i].profile_image = singleSnapshot.getValue(User::class.java)?.profile_image

                        mMessagesList!![i].name = singleSnapshot.getValue(User::class.java)?.name
//                        mAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
            }
        }
    }


    private fun initMessagesList() {
        mAdapter = ChatMessageListAdapter(requireContext(), mMessagesList)
//        mListView!!.setAdapter(mAdapter)
//        mListView!!.setSelection(mAdapter.getCount() - 1) //scroll to the bottom of the list
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

    private fun hideSoftKeyboard() {
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
    */

    /*
            ----------------------------- Firebase setup ---------------------------------
    */
    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
//            val intent = Intent(this@ChatroomActivity, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
        } else {
            Log.d(
                "ChatRoom Fragment",
                "checkAuthenticationState: user is authenticated."
            )
        }
    }

    private fun setupFirebaseAuth() {
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Log.d(
                    "ChatRoom Fragment",
                    "onAuthStateChanged:signed_in:" + user.uid
                )
            } else {
                // User is signed out
                Log.d(
                    "ChatRoom Fragment",
                    "onAuthStateChanged:signed_out"
                )
                Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this@ChatroomActivity, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
            }
            // ...
        }
    }

    /**
     * upadte the total number of message the user has seen
     */
    private fun updateNumMessages(numMessages: Int) {
        val reference = FirebaseDatabase.getInstance().reference
        mChatroom?.chatroom_id?.let {
            reference
                .child(getString(R.string.dbnode_chatrooms))
                .child(it)
                .child(getString(R.string.field_users))
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(getString(R.string.field_last_message_seen))
                .setValue(numMessages.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mMessagesReference!!.removeEventListener(mValueEventListener)
    }

    var mValueEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            getChatroomMessages()

            //get the number of messages currently in the chat and update the database
            val numMessages = dataSnapshot.childrenCount.toInt()
            updateNumMessages(numMessages)
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private fun enableChatroomListener() {
        /*
            ---------- Listener that will watch the 'chatroom_messages' node ----------
         */
        mMessagesReference =
            mChatroom?.chatroom_id?.let {
                FirebaseDatabase.getInstance().reference.child(getString(R.string.dbnode_chatrooms))
                    .child(it)
                    .child(getString(R.string.field_chatroom_messages))
            }
        mMessagesReference?.addValueEventListener(mValueEventListener)
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener!!)
//        courses.pluralsight.com.tabianconsulting.ChatroomActivity.isActivityRunning = true
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener!!)
        }
//        courses.pluralsight.com.tabianconsulting.ChatroomActivity.isActivityRunning = false
    }

}