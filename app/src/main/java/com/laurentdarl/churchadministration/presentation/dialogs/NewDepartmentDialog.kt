package com.laurentdarl.churchadministration.presentation.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.FirebaseDatabase
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.presentation.admin.AdminFragment

class NewDepartmentDialog: DialogFragment() {
    private val TAG = "NewDepartmentDialog"

    //widgets
    private var mNewDepartment: EditText? = null

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.dialog_add_department, container, false)
        mNewDepartment = view.findViewById<View>(R.id.input_new_department) as EditText
        val confirmDialog = view.findViewById<View>(R.id.dialogConfirm) as TextView
        confirmDialog.setOnClickListener {
            if (!isEmpty(mNewDepartment!!.text.toString())) {
                Log.d(TAG, "onClick: adding new department to the list.")
                val reference = FirebaseDatabase.getInstance().reference
                reference
                    .child(getString(R.string.dbnode_departments))
                    .child(mNewDepartment!!.text.toString())
                    .setValue(mNewDepartment!!.text.toString())
                dialog!!.dismiss()
                (activity as AdminFragment?)?.getDepartments()
            }
        }
        return view
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return string == ""
    }
}