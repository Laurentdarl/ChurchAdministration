package com.laurentdarl.churchadministration.presentation.registration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.data.utility.UniversalImageLoader
import com.laurentdarl.churchadministration.databinding.FragmentSignedInBinding
import com.nostra13.universalimageloader.core.ImageLoader

class SignedInFragment : Fragment() {

    private var _binding: FragmentSignedInBinding? = null
    private val binding get() = _binding!!
    private val TAG = "SignedInActivity"
    //Firebase
    private var mAuthListener: AuthStateListener? = null
    //vars
    var isActivityRunning = false
    private var mIsAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignedInBinding.inflate(layoutInflater)

        setupFirebaseAuth()
        isAdmin()
        initFCM()
        initImageLoader()
        getPendingIntent()

        // Inflate the layout for this fragment
        return binding.root
    }


    private fun initFCM() {
        val token: Task<String> = FirebaseMessaging.getInstance().token
        Log.d(
            "SignedIn Fragment",
            "initFCM: token: $token"
        )
        sendRegistrationToServer(token)
    }


    private fun getPendingIntent() {
        Log.d(
            "SignedIn Fragment",
            "getPendingIntent: checking for pending intents."
        )
//        val intent: Intent = getIntent()
//        if (intent.hasExtra(getString(R.string.intent_chatroom))) {
//            Log.d(
//                "SignedIn Fragment",
//                "getPendingIntent: pending intent detected."
//            )
//
//            //get the chatroom
////            val chatroom: Chatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom))
////            //navigate to the chatoom
////            val chatroomIntent = Intent(this@SignedInActivity, ChatroomActivity::class.java)
////            chatroomIntent.putExtra(getString(R.string.intent_chatroom), chatroom)
////            startActivity(chatroomIntent)
//        }
    }

    private fun isAdmin() {
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_users))
            .orderByChild(getString(R.string.field_user_id))
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(
                    "SignedIn Fragment",
                    "onDataChange: datasnapshot: $dataSnapshot"
                )
                val singleSnapshot = dataSnapshot.children.iterator().next()
                val securityLevel: Int =
                    singleSnapshot.getValue(User::class.java)?.security_level!!.toInt()
                if (securityLevel == 10) {
                    Log.d(
                        "SignedIn Fragment",
                        "onDataChange: user is an admin."
                    )
                    mIsAdmin = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: Task<String>) {
        Log.d(
            "SignedIn Fragment",
            "sendRegistrationToServer: sending token to server: $token"
        )
        val reference = FirebaseDatabase.getInstance().reference
        reference.child(getString(R.string.dbnode_users))
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(getString(R.string.field_messaging_token))
            .setValue(token)
    }


    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }


    private fun checkAuthenticationState() {
        Log.d(
            "SignedIn Fragment",
            "checkAuthenticationState: checking authentication state."
        )
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(
                "SignedIn Fragment",
                "checkAuthenticationState: user is null, navigating back to login screen."
            )
//            val intent = Intent(this@SignedInActivity, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
        } else {
            Log.d(
                "SignedIn Fragment",
                "checkAuthenticationState: user is authenticated."
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        return when (item.itemId) {
            R.id.optionSignOut -> {
                signOut()
                true
            }
            R.id.optionAccountSettings -> {
//                intent = Intent(this@SignedInActivity, SettingsActivity::class.java)
//                startActivity(intent)
                true
            }
            R.id.optionChat -> {
//                intent = Intent(this@SignedInActivity, ChatActivity::class.java)
//                startActivity(intent)
                true
            }
            R.id.optionAdmin -> {
                if (mIsAdmin) {
//                    intent = Intent(this@SignedInActivity, AdminActivity::class.java)
//                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "You're not an Admin", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    /**
     * init universal image loader
     */
    private fun initImageLoader() {
        val imageLoader = UniversalImageLoader()
        ImageLoader.getInstance().init(imageLoader.getConfig())
    }

    /**
     * Sign out the current user
     */
    private fun signOut() {
        Log.d("SignedIn Fragment", "signOut: signing out")
        FirebaseAuth.getInstance().signOut()
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
         */
    private fun setupFirebaseAuth() {
        Log.d(
            "SignedIn Fragment",
            "setupFirebaseAuth: started."
        )
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(
                    "SignedIn Fragment",
                    "onAuthStateChanged:signed_in:" + user.uid
                )
            } else {
                Log.d(
                    "SignedIn Fragment",
                    "onAuthStateChanged:signed_out"
                )
//                val intent = Intent(this@SignedInActivity, LoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                finish()
            }
            // ...
        }
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener!!)
        isActivityRunning = true
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener!!)
        }
        isActivityRunning = false
    }
//
//    override fun onNewToken(p0: String) {
//        super.onNewToken(p0)
//        // Get updated InstanceID token.
//        // If you want to send messages to this application instance or
//        // manage this apps subscriptions on the server side, send the
//        // Instance ID token to your app server.
//        Log.e("Refreshed token:",p0)
//
//    }

}