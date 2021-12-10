package com.laurentdarl.churchadministration.presentation.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.FirebaseDatabase
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.presentation.chat.ChatFragment

class DeleteChatroomDialog: DialogFragment() {
    private val TAG = "DeleteChatroomDialog"

    //create a new bundle and set the arguments to avoid a null pointer
    fun DeleteChatroomDialog() {
//        super()
        arguments = Bundle()
    }

    private var mChatroomId: String? = null


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: started")
        mChatroomId = arguments?.getString(getString(R.string.field_chatroom_id))
        if (mChatroomId != null) {
            Log.d(TAG, "onCreate: got the chatroom id: $mChatroomId")
        }
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.dialog_delete_chatroom, container, false)
        val delete = view.findViewById<View>(R.id.confirm_delete) as TextView
        delete.setOnClickListener {
            if (mChatroomId != null) {
                Log.d(TAG, "onClick: deleting chatroom: $mChatroomId")
                val reference = FirebaseDatabase.getInstance().reference
                reference.child(getString(R.string.dbnode_chatrooms))
                    .child(mChatroomId!!)
                    .removeValue()
                (activity as ChatFragment).getChatrooms()
                dialog?.dismiss()
            }
        }
        val cancel = view.findViewById<View>(R.id.cancel) as TextView
        cancel.setOnClickListener {
            Log.d(TAG, "onClick: canceling deletion of chatroom")
            dialog?.dismiss()
        }
        return view
    }

}