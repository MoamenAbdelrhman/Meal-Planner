package com.example.foodplanner.core.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.foodplanner.R
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.core.model.remote.FailureReason
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.main.view.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object CreateMaterialAlertDialogBuilder {
    fun createMaterialAlertDialogBuilderOkCancel(
        context: Context,
        title: String,
        message: String,
        positiveBtnMsg: String,
        negativeBtnMsg: String,
        positiveBtnFun: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveBtnMsg) { dialog, _ ->
                positiveBtnFun()
                dialog.dismiss()
            }
            .setNegativeButton(negativeBtnMsg) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun createMaterialAlertDialogBuilderOk(
        context: Context,
        title: String,
        message: String,
        positiveBtnMsg: String,
        positiveBtnFun: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveBtnMsg) { dialog, _ ->
                positiveBtnFun()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun createGuestLoginDialog(context: Context) {

        val messageTextView = TextView(context).apply {
            text = "You need to log in to access this feature."
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(36,36,36,36)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(messageTextView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Login", null)
            .setCancelable(true)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.dialog_background))
        val widthInDp = 300
        val scale = context.resources.displayMetrics.density
        val widthInPixels = (widthInDp * scale + 0.5f).toInt()
        dialog.window?.setLayout(widthInPixels, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.mycolor))
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            positiveButton.setPadding(32, 16, 32, 16)
            positiveButton.textSize = 16f
            positiveButton.setOnClickListener {
                val intent = Intent(context, AuthActivity::class.java)
                context.startActivity(intent)
                dialog.dismiss()
            }
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.let { negativeButton ->
            negativeButton.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent))
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.black))
            negativeButton.setBackgroundResource(R.drawable.outlined_button_background)
            negativeButton.setPadding(32, 16, 32, 16)
            negativeButton.textSize = 14f
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
    }



    fun createConfirmRemovalDialog(
        context: Context,
        message: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Confirm Removal")
            .setMessage(message)
            .setPositiveButton("Yes", null)
            .setNegativeButton("No", null)
            .setCancelable(true)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.dialog_background))
        val widthInDp = 300
        val scale = context.resources.displayMetrics.density
        val widthInPixels = (widthInDp * scale + 0.5f).toInt()
        dialog.window?.setLayout(widthInPixels, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent))
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.mycolor))
            positiveButton.setBackgroundResource(R.drawable.outlined_button_background)
            positiveButton.setPadding(32, 16, 32, 16)
            positiveButton.textSize = 14f
            positiveButton.setOnClickListener {
                positiveAction()
                dialog.dismiss()
            }
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.let { negativeButton ->
            negativeButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.mycolor))
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.white))
            negativeButton.setPadding(32, 16, 32, 16)
            negativeButton.textSize=14f
            negativeButton.setOnClickListener {
                negativeAction()
                dialog.dismiss()
            }
        }
    }
}