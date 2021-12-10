package com.laurentdarl.churchadministration.presentation.dialogs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.laurentdarl.churchadministration.R
import java.lang.ClassCastException

class ChangePhotoDialog: DialogFragment() {
    private val TAG = "ChangePhotoDialog"

    val CAMERA_REQUEST_CODE = 5467 //random number

    val PICKFILE_REQUEST_CODE = 8352 //random number


    interface OnPhotoReceivedListener {
        fun getImagePath(imagePath: Uri?)
        fun getImageBitmap(bitmap: Bitmap?)
    }

    var mOnPhotoReceived: OnPhotoReceivedListener? = null

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.dialog_changephoto, container, false)

        //Initialize the textview for choosing an image from memory
        val selectPhoto = view.findViewById<View>(R.id.dialogChoosePhoto) as TextView
        selectPhoto.setOnClickListener {
            Log.d(TAG, "onClick: accessing phones memory.")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICKFILE_REQUEST_CODE)
        }

        //Initialize the textview for choosing an image from memory
        val takePhoto = view.findViewById<View>(R.id.dialogOpenCamera) as TextView
        takePhoto.setOnClickListener {
            Log.d(TAG, "onClick: starting camera")
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /*
        Results when selecting new image from phone memory
         */if (requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data
            Log.d(TAG, "onActivityResult: image: $selectedImageUri")

            //send the bitmap and fragment to the interface
            mOnPhotoReceived!!.getImagePath(selectedImageUri)
            dialog!!.dismiss()
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "onActivityResult: done taking a photo.")
            val bitmap: Bitmap? = data?.extras!!["data"] as Bitmap?
            mOnPhotoReceived!!.getImageBitmap(bitmap)
            dialog!!.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mOnPhotoReceived = activity as OnPhotoReceivedListener?
        } catch (e: ClassCastException) {
            Log.e(TAG, "onAttach: ClassCastException", e.cause)
        }
        super.onAttach(requireContext())
    }

}