package com.example.foodplanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView

class AboutDeveloperFragment : Fragment() {

    private lateinit var email: TextView
    private lateinit var linkedIn: TextView
    private lateinit var gitHub: TextView

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about_developer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE

        email = view.findViewById(R.id.userEmail)
        linkedIn = view.findViewById(R.id.userLinkedIn)
        gitHub = view.findViewById(R.id.userGitHub)

        email.setOnClickListener {
            sendEmail("moameny84@gmail.com")
        }

        linkedIn.setOnClickListener {
            openLink("https://www.linkedin.com/in/MoamenAbdelrhman")
        }

        gitHub.setOnClickListener {
            openLink("https://github.com/MoamenAbdelrhman")
        }

    }

    private fun sendEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        startActivity(intent)
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavigationView.visibility = View.VISIBLE
    }

}