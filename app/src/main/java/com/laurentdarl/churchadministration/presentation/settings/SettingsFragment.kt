package com.laurentdarl.churchadministration.presentation.settings

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.databinding.FragmentSettingsBinding
import com.laurentdarl.churchadministration.presentation.dialogs.ChangePhotoDialog
import com.nostra13.universalimageloader.core.ImageLoader
import java.io.ByteArrayOutputStream
import java.io.IOException
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.google.firebase.database.Query
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.OnProgressListener
import android.graphics.BitmapFactory
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.domain.fcm.FilePaths


open class SettingsFragment : Fragment(), ChangePhotoDialog.OnPhotoReceivedListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val TAG = "SettingsActivity"

    override fun getImagePath(imagePath: Uri?) {
        if (imagePath.toString() != "") {
            mSelectedImageBitmap = null
            mSelectedImageUri = imagePath
            Log.d(TAG, "getImagePath: got the image uri: $mSelectedImageUri")
            ImageLoader.getInstance().displayImage(imagePath.toString(), mProfileImage)
        }
    }

    override fun getImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            mSelectedImageUri = null
            mSelectedImageBitmap = bitmap
            Log.d(TAG, "getImageBitmap: got the image bitmap: $mSelectedImageBitmap")
            mProfileImage!!.setImageBitmap(bitmap)
        }
    }


    private val DOMAIN_NAME = "tabian.ca"
    private val REQUEST_CODE = 1234
    private val MB_THRESHHOLD = 5.0
    private val MB = 1000000.0


    //firebase
    private var mAuthListener: AuthStateListener? = null

    //widgets
    private var mEmail: EditText? = null  //widgets
    private var mCurrentPassword: EditText? = null  //widgets
    private var mName: EditText? = null  //widgets
    private var mPhone: EditText? = null
    private var mProfileImage: ImageView? = null
    private var mSave: Button? = null
    private var mProgressBar: ProgressBar? = null
    private var mResetPasswordLink: TextView? = null

    //vars
    private var mStoragePermissions = false
    private var mSelectedImageUri: Uri? = null
    private var mSelectedImageBitmap: Bitmap? = null
    private var mBytes: ByteArray? = null
    private var progress = 0.0
    var isActivityRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(layoutInflater)

        mEmail = binding.inputEmail
        mCurrentPassword = binding.inputPassword
        mSave = binding.btnSave
        mProgressBar = binding.progressBar
        mResetPasswordLink = binding.changePassword
        mName = binding.inputName
        mPhone = binding.inputPhone
        mProfileImage = binding.profileImage

        verifyStoragePermissions()
        setupFirebaseAuth()
        setCurrentEmail()
        init()
        hideSoftKeyboard(requireContext(), requireView())

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun init() {
        getUserAccountData()
        mSave!!.setOnClickListener {
            Log.d(
                "Settings Fragment",
                "onClick: attempting to save settings."
            )

            //see if they changed the email
            if (mEmail!!.text.toString() != FirebaseAuth.getInstance().currentUser!!.email) {
                //make sure email and current password fields are filled
                if ((!isEmpty(mEmail!!.text.toString())
                            && !isEmpty(mCurrentPassword!!.text.toString()))
                ) {

                    //verify that user is changing to a company email address
                    if (isValidDomain(mEmail!!.text.toString())) {
                        editUserEmail()
                    } else {
                        Toast.makeText(requireContext(), "Invalid Domain", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Email and Current Password Fields Must be Filled to Save",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


            /*
                ------ METHOD 1 for changing database data (proper way in this scenario) -----
                 */
            val reference = FirebaseDatabase.getInstance().reference
            /*
                ------ Change Name -----
                 */if (mName!!.text.toString() != "") {
            reference.child(getString(R.string.dbnode_users))
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(getString(R.string.field_name))
                .setValue(mName!!.text.toString())
        }


            /*
                ------ Change Phone Number -----
                 */if (mPhone!!.text.toString() != "") {
            reference.child(getString(R.string.dbnode_users))
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child(getString(R.string.field_phone))
                .setValue(mPhone!!.text.toString())
        }

            /*
                ------ Upload the New Photo -----
                 */if (mSelectedImageUri != null) {
            uploadNewPhoto(mSelectedImageUri)
        } else if (mSelectedImageBitmap != null) {
            uploadNewPhoto(mSelectedImageBitmap)
        }
            Toast.makeText(requireContext(), "saved", Toast.LENGTH_SHORT).show()
        }
        mResetPasswordLink!!.setOnClickListener {
            Log.d(
                "Settings Fragment",
                "onClick: sending password reset link"
            )

            /*
                ------ Reset Password Link -----
                */sendResetPasswordLink()
        }
        mProfileImage!!.setOnClickListener {
            if (mStoragePermissions) {
                val dialog = ChangePhotoDialog()
                dialog.show(childFragmentManager, getString(R.string.dialog_change_photo))
            } else {
                verifyStoragePermissions()
            }
        }
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageUri***
     * @param imageUri
     */
    private fun uploadNewPhoto(imageUri: Uri?) {
        /*
            upload a new profile photo to firebase storage
         */
        Log.d(
            "Settings Fragment",
            "uploadNewPhoto: uploading new profile photo to firebase storage."
        )

        //Only accept image sizes that are compressed to under 5MB. If that's not possible
        //then do not allow image to be uploaded
        val resize = BackgroundImageResize(null)
        resize.execute(imageUri)
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageBitmap***
     * @param imageBitmap
     */
    private fun uploadNewPhoto(imageBitmap: Bitmap?) {
        /*
            upload a new profile photo to firebase storage
         */
        Log.d(
            "Settings Fragment",
            "uploadNewPhoto: uploading new profile photo to firebase storage."
        )

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        val resize = BackgroundImageResize(imageBitmap)
        val uri: Uri? = null
        resize.execute(uri)
    }

    /**
     * 1) doinBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */



    inner class BackgroundImageResize(bm: Bitmap?) :
        AsyncTask<Uri?, Int?, ByteArray?>() {
        var mBitmap: Bitmap? = null
        override fun onPreExecute() {
            super.onPreExecute()
            showDialog()
        }

        override fun onPostExecute(result: ByteArray?) {
            super.onPostExecute(result)
            hideDialog()
            mBytes = result
            //execute the upload
            executeUploadTask()
        }

        init {
            if (bm != null) {
                mBitmap = bm
            }
        }

        override fun doInBackground(vararg params: Uri?): ByteArray? {
            if (mBitmap == null) {
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(
                        context?.contentResolver,
                        params[0]
                    )
                } catch (e: IOException) {
                    Log.e(
                        "Settings Fragment",
                        "doInBackground: IOException: ",
                        e.cause
                    )
                }
            }
            var bytes: ByteArray? = null
            for (i in 1..10) {
                if (i == 10) {
                    break
                }
                bytes = getBytesFromBitmap(mBitmap, 100 / i)
                Log.d(
                    "Settings Fragment",
                    "doInBackground: megabytes: (" + (11 - i) + "0%) " + bytes!!.size / 1000000.0 + " MB"
                )
                if (bytes.size / 1000000.0 < 5.0) {
                    return bytes
                }
            }
            return bytes
        }

        // convert from bitmap to byte array
        private fun getBytesFromBitmap(bitmap: Bitmap?, quality: Int): ByteArray? {
            val stream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            return stream.toByteArray()
        }
    }

    private fun hideDialog() {
        if (mProgressBar!!.visibility == View.VISIBLE) {
            mProgressBar!!.visibility = View.INVISIBLE
        }
    }

    private fun showDialog() {
        mProgressBar!!.visibility = View.INVISIBLE
    }

    private fun executeUploadTask() {
        showDialog()
        val filePaths = FilePaths()
        //specify where the photo will be stored
        val storageReference = FirebaseStorage.getInstance().reference
            .child(
                filePaths.FIREBASE_IMAGE_STORAGE.toString() + "/" + FirebaseAuth.getInstance().currentUser!!
                    .uid
                        + "/profile_image"
            ) //just replace the old image with the new one
        if (mBytes!!.size / MB < MB_THRESHHOLD) {

            // Create file metadata including the content type
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpg")
                .setContentLanguage("en") //see nodes below
                /*
                    Make sure to use proper language code ("English" will cause a crash)
                    I actually submitted this as a bug to the Firebase github page so it might be
                    fixed by the time you watch this video. You can check it out at https://github.com/firebase/quickstart-unity/issues/116
                     */
                .setCustomMetadata("Mitch's special meta data", "JK nothing special here")
                .setCustomMetadata("location", "Iceland")
                .build()
            //if the image size is valid then we can submit to database
            var uploadTask: UploadTask? = null
            uploadTask = storageReference.putBytes((mBytes!!), metadata)
            //uploadTask = storageReference.putBytes(mBytes); //without metadata
            uploadTask.addOnSuccessListener(OnSuccessListener { taskSnapshot -> //Now insert the download url into the firebase database
                val firebaseURL: Uri? = taskSnapshot.uploadSessionUri
                Toast.makeText(requireContext(), "Upload Success", Toast.LENGTH_SHORT).show()
                Log.d(
                    "Settings Fragment",
                    "onSuccess: firebase download url : $firebaseURL"
                )
                FirebaseDatabase.getInstance().reference
                    .child(getString(R.string.dbnode_users))
                    .child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .child(getString(R.string.field_profile_image))
                    .setValue(firebaseURL.toString())
                hideDialog()
            }).addOnFailureListener {
                Toast.makeText(requireContext(), "could not upload photo", Toast.LENGTH_SHORT)
                    .show()
                hideDialog()
            }.addOnProgressListener { taskSnapshot ->
                val currentProgress =
                    ((100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toDouble()
                if (currentProgress > (progress + 15)) {
                    progress =
                        ((100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toDouble()
                    Log.d(
                        "Settings Fragment",
                        "onProgress: Upload is $progress% done"
                    )
                    Toast.makeText(requireContext(), "$progress%", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d(
                "Settings Fragment",
                "Image is too Large"
            )
        }
    }

    private fun getUserAccountData() {
        Log.d(
            "Settings Fragment",
            "getUserAccountData: getting the user's account information"
        )
        val reference = FirebaseDatabase.getInstance().reference

        /*
            ---------- QUERY Method 1 ----------
         */
        val query1 = reference.child(getString(R.string.dbnode_users))
            .orderByKey()
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query1.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                //this loop will return a single result
                for (singleSnapshot: DataSnapshot in dataSnapshot.children) {
                    Log.d(
                        "Settings Fragment",
                        ("onDataChange: (QUERY METHOD 1) found user: "
                                + singleSnapshot.getValue(User::class.java).toString())
                    )
                    val user: User? = singleSnapshot.getValue(User::class.java)
                    if (user != null) {
                        binding.inputName.setText(user.name)
                        binding.inputPhone.setText(user.phone)
                        ImageLoader.getInstance().displayImage(user.profile_image, mProfileImage)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        /*
            ---------- QUERY Method 2 ----------
         */
        val query2 = reference.child(getString(R.string.dbnode_users))
            .orderByChild(getString(R.string.field_user_id))
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                //this loop will return a single result
                for (singleSnapshot: DataSnapshot in dataSnapshot.children) {
                    Log.d(
                        "Settings Fragment",
                        ("onDataChange: (QUERY METHOD 2) found user: "
                                + singleSnapshot.getValue(User::class.java).toString())
                    )
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        mEmail!!.setText(FirebaseAuth.getInstance().currentUser!!.email)
    }


    /**
     * Generalized method for asking permission. Can pass any array of permissions
     */
    fun verifyStoragePermissions() {
        Log.d(
            "Settings Fragment",
            "verifyPermissions: asking user for permissions."
        )
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        if ((ContextCompat.checkSelfPermission(
                requireContext(),
                permissions[0]
            ) == PackageManager.PERMISSION_GRANTED
                    ) && (ContextCompat.checkSelfPermission(
                requireContext(),
                permissions[1]
            ) == PackageManager.PERMISSION_GRANTED
                    ) && (ContextCompat.checkSelfPermission(
                requireContext(),
                permissions[2]
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            mStoragePermissions = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissions,
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        Log.d(
            "Settings Fragment",
            "onRequestPermissionsResult: requestCode: $requestCode"
        )
        when (requestCode) {
            REQUEST_CODE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(
                    "Settings Fragment",
                    "onRequestPermissionsResult: User has allowed permission to access: " + permissions[0]
                )
            }
        }
    }


    private fun sendResetPasswordLink() {
        FirebaseAuth.getInstance()
            .sendPasswordResetEmail((FirebaseAuth.getInstance().currentUser!!.email)!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        "Settings Fragment",
                        "onComplete: Password Reset Email sent."
                    )
                    Toast.makeText(
                        requireContext(), "Sent Password Reset Link to Email",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d(
                        "Settings Fragment",
                        "onComplete: No user associated with that email."
                    )
                    Toast.makeText(
                        requireContext(), "No User Associated with that Email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun editUserEmail() {
        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.
        showDialog()
        val credential = EmailAuthProvider
            .getCredential(
                (FirebaseAuth.getInstance().currentUser!!.email)!!,
                mCurrentPassword!!.text.toString()
            )
        Log.d(
            "Settings Fragment",
            ("editUserEmail: reauthenticating with:  \n email " + FirebaseAuth.getInstance().currentUser!!
                .email
                    + " \n passowrd: " + mCurrentPassword!!.text.toString())
        )
        FirebaseAuth.getInstance().currentUser!!.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        "Settings Fragment",
                        "onComplete: reauthenticate success."
                    )

                    //make sure the domain is valid
                    if (isValidDomain(mEmail!!.text.toString())) {

                        ///////////////////now check to see if the email is not already present in the database
                        FirebaseAuth.getInstance()
                            .fetchSignInMethodsForEmail(mEmail!!.text.toString())
                            .addOnCompleteListener { fetchTask ->
                                if (fetchTask.isSuccessful) {
                                    val providerResult = fetchTask.result
                                    val isNewAccount =
                                        providerResult?.signInMethods!!.contains((credential.provider))
                                    //Do whatever you need depending if its or not a new account
                                    FirebaseAuth.getInstance()
                                        .signInWithCredential(credential)
                                        .addOnCompleteListener { signInTask ->
                                            if (signInTask.isSuccessful) {
                                                val authResult = signInTask.result
                                                val emailVerified =
                                                    authResult?.user?.isEmailVerified
//                                                val user = authResult?.user.toUser().copy(photoUrl = userData.photoUrl)
                                                val providers = authResult?.user?.providerData

                                                //Do whatever you need with your data now

                                                if (!emailVerified!!) {
                                                    //Manage your email verification
                                                    Log.d(
                                                        "Settings Fragment",
                                                        "onComplete: User email address updated."
                                                    )
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Updated email",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    sendVerificationEmail()
                                                    FirebaseAuth.getInstance().signOut()

                                                }
                                            } else {
                                                //Manage error
                                                Log.d(
                                                    "Settings Fragment",
                                                    "onComplete: Could not update email."
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    "unable to update email",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            hideDialog()
                                        }
                                } else {
//                                    Manage error
                                    hideDialog()
                                    Toast.makeText(
                                        requireContext(),
                                        "unable to update email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "you must use a company email",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d(
                        "Settings Fragment",
                        "onComplete: Incorrect Password"
                    )
                    Toast.makeText(requireContext(), "Incorrect Password", Toast.LENGTH_SHORT)
                        .show()
                    hideDialog()
                }
            }.addOnFailureListener {
                hideDialog()
                Toast.makeText(
                    requireContext(),
                    "“unable to update email”",
                    Toast.LENGTH_SHORT
                )
                    .show()
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

    private fun setCurrentEmail() {
        Log.d(
            "Settings Fragment",
            "setCurrentEmail: setting current email to EditText field"
        )
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Log.d(
                "Settings Fragment",
                "setCurrentEmail: user is NOT null."
            )
            val email = user.email
            Log.d(
                "Settings Fragment",
                "setCurrentEmail: got the email: $email"
            )
            mEmail!!.setText(email)
        }
    }

    /**
     * Returns True if the user's email contains '@tabian.ca'
     * @param email
     * @return
     */
    private fun isValidDomain(email: String): Boolean {
        Log.d(
            "Settings Fragment",
            "isValidDomain: verifying email has correct domain: $email"
        )
        val domain = email.substring(email.indexOf("@") + 1).toLowerCase()
        Log.d(
            "Settings Fragment",
            "isValidDomain: users domain: $domain"
        )
        return (domain == DOMAIN_NAME)
    }

    private fun hideSoftKeyboard(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return (string == "")
    }

    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        Log.d(
            "Settings Fragment",
            "checkAuthenticationState: checking authentication state."
        )
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(
                "Settings Fragment",
                "checkAuthenticationState: user is null, navigating back to login screen."
            )
//            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
        } else {
            Log.d(
                "Settings Fragment",
                "checkAuthenticationState: user is authenticated."
            )
        }
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
         */
    private fun setupFirebaseAuth() {
        Log.d(
            "Settings Fragment",
            "setupFirebaseAuth: started."
        )
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Log.d(
                    "Settings Fragment",
                    "onAuthStateChanged:signed_in:" + user.uid
                )
                //toastMessage("Successfully signed in with: " + user.getEmail());
            } else {
                // User is signed out
                Log.d(
                    "Settings Fragment",
                    "onAuthStateChanged:signed_out"
                )
                Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
            }
            // ...
        }
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener((mAuthListener)!!)
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