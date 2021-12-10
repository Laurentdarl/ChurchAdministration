package com.laurentdarl.churchadministration.presentation.registration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.widget.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.utility.UniversalImageLoader
import com.laurentdarl.churchadministration.databinding.FragmentLoginBinding
import com.laurentdarl.churchadministration.presentation.dialogs.PasswordResetDialog
import com.laurentdarl.churchadministration.presentation.dialogs.ResendVerificationDialog
import com.nostra13.universalimageloader.core.ImageLoader
import android.app.Activity
import android.content.Context
import android.view.*
import android.view.inputmethod.InputMethodManager


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val TAG = "LoginActivity"

    //constants
    private val ERROR_DIALOG_REQUEST = 9001

    //Firebase
    private var mAuthListener: AuthStateListener? = null

    // widgets
    private var mEmail: EditText? = null  // widgets
    private var mPassword: EditText? = null
    private var mProgressBar: ProgressBar? = null
    var isActivityRunning = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(layoutInflater)

        mEmail = binding.email
        mPassword = binding.password
        mProgressBar = binding.progressBar

        setupFirebaseAuth()
        initImageLoader()
        if (servicesOK()) {
            init()
        }
        hideSoftKeyboard(requireContext(), requireView())

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun init() {
        val signIn = binding.emailSignInButton
        signIn.setOnClickListener { //check if the fields are filled out
            if (!isEmpty(mEmail!!.text.toString())
                && !isEmpty(mPassword!!.text.toString())
            ) {
                Log.d(
                    "LOGIN Fragment",
                    "onClick: attempting to authenticate."
                )
                showDialog()
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    mEmail!!.text.toString(),
                    mPassword!!.text.toString()
                )
                    .addOnCompleteListener { hideDialog() }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Authentication Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        hideDialog()
                    }
            } else {
                Toast.makeText(
                    requireContext(),
                    "You didn't fill in all the fields.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val register = binding.linkRegister
        register.setOnClickListener {
//            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
//            startActivity(intent)
        }
        val resetPassword = binding.forgotPassword
        resetPassword.setOnClickListener {
            val dialog = PasswordResetDialog()
            dialog.show(childFragmentManager, "dialog_password_reset")
        }
        val resendEmailVerification = binding.resendVerificationEmail
        resendEmailVerification.setOnClickListener {
            val dialog = ResendVerificationDialog()
            dialog.show(childFragmentManager, "dialog_resend_email_verification")

        }
    }


    fun servicesOK(): Boolean {
        Log.d(
            "LOGIN Fragment",
            "servicesOK: Checking Google Services."
        )
        val isAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())
        if (isAvailable == ConnectionResult.SUCCESS) {
            //everything is ok and the user can make mapping requests
            Log.d(
                "LOGIN Fragment",
                "servicesOK: Play Services is OK"
            )
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(isAvailable)) {
            //an error occurred, but it's resolvable
            Log.d(
                "LOGIN Fragment",
                "servicesOK: an error occurred, but it's resolvable."
            )
            val dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                requireActivity(),
                isAvailable,
                ERROR_DIALOG_REQUEST
            )
            dialog.show()
        } else {
            Toast.makeText(requireContext(), "Can't connect to mapping services", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    /**
     * init universal image loader
     */
    private fun initImageLoader() {
        val imageLoader = UniversalImageLoader()
        ImageLoader.getInstance().init(imageLoader.getConfig())
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return string == ""
    }


    private fun showDialog() {
        mProgressBar!!.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        if (mProgressBar!!.visibility == View.VISIBLE) {
            mProgressBar!!.visibility = View.INVISIBLE
        }
    }

    private fun hideSoftKeyboard(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }



    /*
        ----------------------------- Firebase setup ---------------------------------
     */
    private fun setupFirebaseAuth() {
        Log.d(
            "LOGIN Fragment",
            "setupFirebaseAuth: started."
        )
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {

                //check if email is verified
                if (user.isEmailVerified) {
                    Log.d(
                        "LOGIN Fragment",
                        "onAuthStateChanged:signed_in:" + user.uid
                    )
                    Toast.makeText(
                        requireContext(),
                        "Authenticated with: " + user.email,
                        Toast.LENGTH_SHORT
                    ).show()
//                    val intent = Intent(this@LoginActivity, SignedInActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//
//                    //check for extras from FCM
//                    if (getIntent().getExtras() != null) {
//                        Log.d(
//                            courses.pluralsight.com.tabianconsulting.LoginActivity.TAG,
//                            "initFCM: found intent extras: " + getIntent().getExtras().toString()
//                        )
//                        for (key in getIntent().getExtras().keySet()) {
//                            val value: Any = getIntent().getExtras().get(key)
//                            Log.d(
//                                courses.pluralsight.com.tabianconsulting.LoginActivity.TAG,
//                                "initFCM: Key: $key Value: $value"
//                            )
//                        }
//                        val data: String = getIntent().getStringExtra("data")
//                        Log.d(
//                            courses.pluralsight.com.tabianconsulting.LoginActivity.TAG,
//                            "initFCM: data: $data"
//                        )
//                    }
//                    startActivity(intent)
//                    finish()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Email is not Verified\nCheck your Inbox",
                        Toast.LENGTH_SHORT
                    ).show()
                    FirebaseAuth.getInstance().signOut()
                }
            } else {
                // User is signed out
                Log.d(
                    "LOGIN Fragment",
                    "onAuthStateChanged:signed_out"
                )
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

}