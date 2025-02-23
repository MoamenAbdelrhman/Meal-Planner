package com.example.foodplanner.auth

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.foodplanner.R
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth


class ForgotPasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnReset: Button
    private lateinit var btnBack: Button
    private lateinit var edtEmail: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var strEmail: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Initializaton
        btnBack =  view.findViewById(R.id.btnForgotPasswordBack)
        btnReset =  view.findViewById(R.id.btnReset)
        edtEmail =  view.findViewById(R.id.edtForgotPasswordEmail)
        progressBar =  view.findViewById(R.id.forgetPasswordProgressbar)

        auth = FirebaseAuth.getInstance()


        //Reset Button Listener
        btnReset.setOnClickListener {
            strEmail = edtEmail.text.toString().trim { it <= ' ' }
            if (!TextUtils.isEmpty(strEmail)) {
                ResetPassword()
            } else {
                edtEmail.error = "Email field can't be empty"
            }
        }


        //Back Button Code
        btnBack.setOnClickListener {
            (activity as AuthActivity).navigateToLogin()
        }
    }

    private fun ResetPassword() {
        progressBar.visibility = View.VISIBLE
        btnReset.visibility = View.INVISIBLE

        auth.sendPasswordResetEmail(strEmail)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Toast.makeText(
                    requireActivity(),
                    "Reset Password link has been sent to your registered Email",
                    Toast.LENGTH_SHORT
                ).show()
                (activity as AuthActivity).navigateToLogin()
            })
            .addOnFailureListener(OnFailureListener { e ->
                Toast.makeText(
                    requireActivity(),
                    "Error :- " + e.message,
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.INVISIBLE
                btnReset.visibility = View.VISIBLE
            })
    }


}