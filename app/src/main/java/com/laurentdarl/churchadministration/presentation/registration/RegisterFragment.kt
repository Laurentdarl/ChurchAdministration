package com.laurentdarl.churchadministration.presentation.registration

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.databinding.FragmentRegisterBinding


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val TAG = "RegisterActivity"

    private val DOMAIN_NAME = "gmail.com"

    //widgets
    private var mEmail: EditText? = null     //widgets
    private var mPassword: EditText? = null  //widgets
    private var mConfirmPassword: EditText? = null
    private var mRegister: Button? = null
    private var mProgressBar: ProgressBar? = null

    //vars
    var isActivityRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(layoutInflater)

        mEmail = binding.inputEmail
        mPassword = binding.inputPassword
        mConfirmPassword = binding.inputConfirmPassword
        mRegister = binding.btnRegister
        mProgressBar = binding.progressBar

        mRegister!!.setOnClickListener(View.OnClickListener {
            Log.d(
                "Register Fragment",
                "onClick: attempting to register."
            )

            //check for null valued EditText fields
            if (!isEmpty(mEmail!!.text.toString())
                && !isEmpty(mPassword!!.text.toString())
                && !isEmpty(mConfirmPassword!!.text.toString())
            ) {

                //check if user has a company email address
                if (isValidDomain(mEmail!!.text.toString())) {

                    //check if passwords match
                    if (doStringsMatch(
                            mPassword!!.text.toString(),
                            mConfirmPassword!!.text.toString()
                        )
                    ) {

                        //Initiate registration task
                        registerNewEmail(
                            mEmail!!.text.toString(),
                            mPassword!!.text.toString()
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Passwords do not Match",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please Register with Company Email",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "You must fill out all the fields",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        hideSoftKeyboard(requireContext(), requireView())


        // Inflate the layout for this fragment
        return binding.root
    }

    /**
     * Register a new email and password to Firebase Authentication
     * @param email
     * @param password
     */
    fun registerNewEmail(email: String, password: String?) {
        showDialog()
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password!!)
            .addOnCompleteListener { task ->
                Log.d(
                    "Register Fragment",
                    "createUserWithEmail:onComplete:" + task.isSuccessful
                )
                if (task.isSuccessful) {
                    Log.d(
                        "Register Fragment",
                        "onComplete: AuthState: " + FirebaseAuth.getInstance().currentUser!!
                            .uid
                    )

                    //send email verificaiton
                    sendVerificationEmail()

                    //insert some default data
                    val user = User()
                    user.name = email.substring(0, email.indexOf("@"))
                    user.phone = "1"
                    user.profile_image = ""
                    user.security_level = "1"
                    user.user_id = FirebaseAuth.getInstance().currentUser!!.uid
                    FirebaseDatabase.getInstance().reference
                        .child(getString(R.string.dbnode_users))
                        .child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .setValue(user)
                        .addOnCompleteListener {
                            FirebaseAuth.getInstance().signOut()

                            //redirect the user to the login screen
                            redirectLoginScreen()
                        }.addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "something went wrong.",
                                Toast.LENGTH_SHORT
                            ).show()
                            FirebaseAuth.getInstance().signOut()

                            //redirect the user to the login screen
                            redirectLoginScreen()
                        }
                }
                if (!task.isSuccessful) {
                    Toast.makeText(
                        requireContext(), "Unable to Register",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                hideDialog()

                // ...
            }
    }

    /**
     * sends an email verification link to the user
     */
    private fun sendVerificationEmail() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Sent Verification Email",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Couldn't Verification Send Email",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Returns True if the user's email contains '@tabian.ca'
     * @param email
     * @return
     */
    private fun isValidDomain(email: String): Boolean {
        Log.d(
            "Register Fragment",
            "isValidDomain: verifying email has correct domain: $email"
        )
        val domain = email.substring(email.indexOf("@") + 1).toLowerCase()
        Log.d(
            "Register Fragment",
            "isValidDomain: users domain: $domain"
        )
        return domain == DOMAIN_NAME
    }

    /**
     * Redirects the user to the login screen
     */
    private fun redirectLoginScreen() {
        Log.d(
            "Register Fragment",
            "redirectLoginScreen: redirecting to login screen."
        )
//        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
//        startActivity(intent)
//        finish()
    }

    /**
     * Return true if @param 's1' matches @param 's2'
     * @param s1
     * @param s2
     * @return
     */
    private fun doStringsMatch(s1: String, s2: String): Boolean {
        return s1 == s2
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

    override fun onStart() {
        super.onStart()
        isActivityRunning = true
    }

    override fun onStop() {
        super.onStop()
        isActivityRunning = false
    }

}