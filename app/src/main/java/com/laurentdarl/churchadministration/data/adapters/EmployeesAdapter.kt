package com.laurentdarl.churchadministration.data.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.presentation.admin.AdminFragment
import com.nostra13.universalimageloader.core.ImageLoader
import java.util.ArrayList


class EmployeesAdapter(context: Context, users: java.util.ArrayList<User?>?) : RecyclerView.Adapter<EmployeesAdapter.ViewHolder?>() {
    private val mUsers: ArrayList<User?>? = users
    private val mContext: Context = context

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profileImage: ImageView = itemView.findViewById<View>(R.id.profile_image) as ImageView
        var name: TextView = itemView.findViewById<View>(R.id.name) as TextView
        var department: TextView = itemView.findViewById<View>(R.id.department) as TextView

        init {
            itemView.setOnClickListener {
                Log.d(
                    EmployeesAdapter.Companion.TAG,
                    "onClick: selected employee: " + (mUsers?.get(adapterPosition))
                )

                //open a dialog for selecting a department
                mUsers?.get(adapterPosition)?.let { it1 ->
                    (mContext as AdminFragment).setDepartmentDialog(
                        it1
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeesAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        //inflate the custom layout
        val view: View = inflater.inflate(R.layout.layout_employee_listitem, parent, false)

        //return a new holder instance
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmployeesAdapter.ViewHolder, position: Int) {
        ImageLoader.getInstance()
            .displayImage(mUsers?.get(position)?.profile_image, holder.profileImage)
        holder.name.text = mUsers?.get(position)?.name
        holder.department.text = mUsers?.get(position)?.department
    }

    companion object {
        private const val TAG = "EmployeesAdapter"
    }

    override fun getItemCount(): Int {
        return mUsers!!.size
    }
}