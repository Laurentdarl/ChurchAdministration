package com.laurentdarl.churchadministration.data.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import com.laurentdarl.churchadministration.data.models.ChatMessage
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener
import java.lang.NullPointerException

class ChatMessageListAdapter(context: Context?, chatMessage: List<ChatMessage>?) : ArrayAdapter<ChatMessage>(
    context!!, 0, chatMessage!!
) {
    private val TAG = "ChatMessageListAdapter"

    private var mLayoutResource = 0
    private var mContext: Context? = null

    fun ChatMessageListAdapter(
        @NonNull context: Context?,
        @LayoutRes resource: Int,
        @NonNull objects: List<ChatMessage?>?
    ) {
        mContext = context
        mLayoutResource = resource

    }
    class ViewHolder {
        var name: TextView? = null
        var message: TextView? = null
        var mProfileImage: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(mContext)
            convertView = inflater.inflate(mLayoutResource, parent, false)
            holder = ViewHolder()
            holder.name = convertView.findViewById<View>(com.laurentdarl.churchadministration.R.id.name) as TextView
            holder.message = convertView.findViewById<View>(com.laurentdarl.churchadministration.R.id.message) as TextView
            holder.mProfileImage = convertView.findViewById<View>(com.laurentdarl.churchadministration.R.id.profile_image) as ImageView
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            holder.name!!.text = ""
            holder.message!!.text = ""
        }
        try {
            //set the message
            holder.message!!.text = getItem(position)?.message

            //set the name
            holder.name!!.text = getItem(position)?.name

            //set the image (make sure to prevent the image 'flash')
            if (holder.mProfileImage!!.tag == null ||
                holder.mProfileImage!!.tag != getItem(position)?.profile_image
            ) {

                //we only load image if prev. URL and current URL do not match, or tag is null
                ImageLoader.getInstance().displayImage(
                    getItem(position)?.profile_image, holder.mProfileImage,
                    SimpleImageLoadingListener()
                )
                holder.mProfileImage!!.tag = getItem(position)?.profile_image
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "getView: NullPointerException: ", e.cause)
        }
        return super.getView(position, convertView, parent)
    }
}