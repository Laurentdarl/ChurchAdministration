package com.laurentdarl.churchadministration.presentation.admin

import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.adapters.EmployeesAdapter
import com.laurentdarl.churchadministration.data.models.User
import com.laurentdarl.churchadministration.data.models.fcm.Data
import com.laurentdarl.churchadministration.data.models.fcm.FirebaseCloudMessage
import com.laurentdarl.churchadministration.data.utility.VerticalSpacingDecorator
import com.laurentdarl.churchadministration.databinding.FragmentAdminBinding
import com.laurentdarl.churchadministration.domain.fcm.FCM
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.NullPointerException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminActivity"
    private val BASE_URL = "https://fcm.googleapis.com/fcm/"

    //widgets
    private var mDepartments: TextView? = null
    private var mAddDepartment: Button? = null
    private  var mSendMessage:android.widget.Button? = null
    private var mRecyclerView: RecyclerView? = null
    private var mMessage: EditText? = null
    private  var mTitle:EditText? = null

    //vars
    private var mDepartmentsList: ArrayList<String>? = null
    private var mSelectedDepartments: MutableSet<String>? = null
    private var mEmployeeAdapter: EmployeesAdapter? = null
    private var mUsers: ArrayList<User?>? = null
    private var mTokens: MutableSet<String>? = null
    private var mServerKey: String? = null
    var isActivityRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminBinding.inflate(layoutInflater)

        mDepartments = binding.broadcastDepartments
        mAddDepartment = binding.addDepartment
        mSendMessage = binding.btnSendMessage
        mRecyclerView = binding.recyclerView
        mMessage = binding.inputMessage
        mTitle = binding.inputTitle

        setupEmployeeList()
        init()
//        hideSoftKeyboard()

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun init() {
        mSelectedDepartments = HashSet()
        mTokens = HashSet()
        /*
            --------- Dialog for selecting departments ---------
         */mDepartments!!.setOnClickListener {
            Log.d(TAG, "onClick: opening departments selector dialog.")
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//            builder.setIcon(R.drawable.ic_departments)
            builder.setTitle("Select Departments:")

            //create an array of the departments
            val departments = arrayOfNulls<String>(
                mDepartmentsList!!.size
            )
            for (i in mDepartmentsList!!.indices) {
                departments[i] = mDepartmentsList!![i]
            }

            //get the departments that are already added to the list
            val checked = BooleanArray(mDepartmentsList!!.size)
            for (i in mDepartmentsList!!.indices) {
                if ((mSelectedDepartments as HashSet<String>).contains(mDepartmentsList!![i])) {
                    checked[i] = true
                }
            }
            builder.setPositiveButton("done",
                DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
            builder.setMultiChoiceItems(departments, checked,
                OnMultiChoiceClickListener { dialog, which, isChecked ->
                    if (isChecked) {
                        Log.d(
                            TAG,
                            "onClick: adding " + mDepartmentsList!![which] + " to the list."
                        )
                        (mSelectedDepartments as HashSet<String>).add(mDepartmentsList!![which])
                    } else {
                        Log.d(
                            TAG,
                            "onClick: removing " + mDepartmentsList!![which] + " from the list."
                        )
                        (mSelectedDepartments as HashSet<String>).remove(mDepartmentsList!![which])
                    }
                })
            val dialog: AlertDialog = builder.create()
            dialog.show()
            dialog.setOnDismissListener(DialogInterface.OnDismissListener {
                Log.d(TAG, "onDismiss: dismissing dialog and refreshing token list.")
                getDepartmentTokens()
            })
        }
        mAddDepartment!!.setOnClickListener {
            Log.d(TAG, "onClick: opening dialog to add new department")
//            val dialog = NewDepartmentDialog()
//            dialog.show(requireActivity(), getString(R.string.dialog_add_department))
        }
        mSendMessage?.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "onClick: attempting to send the message.")
            val message = mMessage!!.text.toString()
            val title: String = mTitle?.text.toString()
            if (!isEmpty(message) && !isEmpty(title)) {

                //send message
                sendMessageToDepartment(title, message)
                mMessage!!.setText("")
                mTitle?.setText("")
            } else {
                Toast.makeText(
                    requireContext(),
                    "Fill out the title and message fields",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        getDepartments()
        getEmployeeList()
        getServerKey()
    }

    private fun sendMessageToDepartment(title: String, message: String) {
        Log.d(TAG, "sendMessageToDepartment: sending message to selected departments.")
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //create the interface
        val fcmAPI: FCM = retrofit.create(FCM::class.java)

        //attach the headers
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Authorization"] = "key=$mServerKey"

        //send the message to all the tokens
        for (token in mTokens!!) {
            Log.d(TAG, "sendMessageToDepartment: sending to token: $token")
            val data = Data()
            data.message = message
            data.title = title
            data.data_type = getString(R.string.data_type_admin_broadcast)
            val firebaseCloudMessage = FirebaseCloudMessage()
            firebaseCloudMessage.data = data
            firebaseCloudMessage.to = token
            val call: Call<ResponseBody?>? = fcmAPI.send(headers, firebaseCloudMessage)
            if (call != null) {
                call.enqueue(object : Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        Log.d(TAG, "onResponse: Server Response: $response")
                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        Log.e(TAG, "onFailure: Unable to send the message." + t.message)
                        Toast.makeText(requireContext(), "error", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    /**
     * Retrieves the server key for the Firebase server.
     * This is required to send FCM messages.
     */
    private fun getServerKey() {
        Log.d(TAG, "getServerKey: retrieving server key.")
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_server))
            .orderByValue()
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: got the server key.")
                val singleSnapshot = dataSnapshot.children.iterator().next()
                mServerKey = singleSnapshot.value.toString()
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    /**
     * Get all the tokens of the users who are in the selected departments
     */
    private fun getDepartmentTokens() {
        Log.d(TAG, "getDepartmentTokens: searching for tokens.")
        mTokens!!.clear() //clear current token list in case admin has change departments
        val reference = FirebaseDatabase.getInstance().reference
        for (department in mSelectedDepartments!!) {
            Log.d(TAG, "getDepartmentTokens: department: $department")
            val query = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_department))
                .equalTo(department)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val token: String = snapshot.getValue(User::class.java)?.messaging_token!!
                        Log.d(
                            TAG, "onDataChange: got a token for user named: "
                                    + snapshot.getValue(User::class.java)!!.name
                        )
                        mTokens!!.add(token)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    fun setDepartmentDialog(user: User) {
        Log.d(TAG, "setDepartmentDialog: setting the department of: " + user.name)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//        builder.setIcon(R.drawable.ic_departments)
        builder.setTitle("Set a Department for " + user.name.toString() + ":")
        builder.setPositiveButton("done",
            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })

        //get the index of the department (if the user has a department assigned)
        var index = -1
        for (i in mDepartmentsList!!.indices) {
            if (mDepartmentsList!!.contains(user.department)) {
                index = i
            }
        }
        val adapter: ListAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1, mDepartmentsList!!
        )
        builder.setSingleChoiceItems(adapter, index,
            DialogInterface.OnClickListener { dialog, which ->
                Toast.makeText(requireContext(), "Department Saved", Toast.LENGTH_SHORT).show()
                val reference = FirebaseDatabase.getInstance().reference
                reference.child(getString(R.string.dbnode_users))
                    .child(user.user_id!!)
                    .child(getString(R.string.field_department))
                    .setValue(mDepartmentsList!![which])
                dialog.dismiss()
                //refresh the list with the new information
                mUsers!!.clear()
                getEmployeeList()
            })
        builder.show()
    }

    /**
     * Get a list of all employees
     * @throws NullPointerException
     */
    @Throws(NullPointerException::class)
    private fun getEmployeeList() {
        Log.d(TAG, "getEmployeeList: getting a list of all employees")
        val reference = FirebaseDatabase.getInstance().reference
        val query: Query = reference.child(getString(R.string.dbnode_users))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val user: User? = snapshot.getValue(User::class.java)
                    if (user != null) {
                        Log.d(TAG, "onDataChange: found a user: " + user.name)
                    }
                    mUsers!!.add(user)
                }
//                mEmployeeAdapter.notifyDataSetChanged()
                getDepartmentTokens()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Setup the list of employees
     */
    private fun setupEmployeeList() {
        mUsers = ArrayList<User?>()
        mEmployeeAdapter = EmployeesAdapter(requireContext(), mUsers)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(requireContext())
        mRecyclerView?.layoutManager = layoutManager
        mRecyclerView?.addItemDecoration(VerticalSpacingDecorator())
//        mRecyclerView?.setAdapter()
    }

    /**
     * Retrieve a list of departments that have been added to the database.
     */
    fun getDepartments() {
        mDepartmentsList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference
        val query: Query = reference.child(getString(R.string.dbnode_departments))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val department = snapshot.value.toString()
                    Log.d(TAG, "onDataChange: found a department: $department")
                    mDepartmentsList!!.add(department)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun onStart() {
        super.onStart()
        isActivityRunning = true
    }

    override fun onStop() {
        super.onStop()
        isActivityRunning = false
    }

    private fun hideSoftKeyboard() {
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
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