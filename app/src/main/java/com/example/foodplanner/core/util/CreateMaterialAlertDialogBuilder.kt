package com.example.foodplanner.core.util

import android.content.Context
import android.content.Intent
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

    fun createFailureResponse(response: Response.Failure, context: Context, action: (() -> Unit)? = null) {
        when (val failureReason = response.reason) {
            is FailureReason.NoInternet -> {
                createMaterialAlertDialogBuilderOkCancel(
                    context,
                    title = "No Internet Connection",
                    message = "Please check your internet connection and try again.",
                    positiveBtnMsg = "Try again",
                    negativeBtnMsg = "Cancel"
                ) {
                    action?.invoke()
                }
            }

            is FailureReason.UnknownError -> {
                val errorMessage = failureReason.error
                createMaterialAlertDialogBuilderOk(
                    context,
                    title = "Unknown Error",
                    message = "An unknown error occurred: $errorMessage",
                    positiveBtnMsg = "Try again"
                ) {
                    action?.invoke()
                }
            }
        }
    }

    fun createGuestLoginDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage("You need to log in to access this feature.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Login", null)
            .setCancelable(true)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.mycolor))
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            positiveButton.setPadding(32, 16, 32, 16)
            positiveButton.setOnClickListener {
                val intent = Intent(context, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                if (context is MainActivity) {
                    context.finish()
                }
            }
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.let { negativeButton ->
            negativeButton.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent))
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.black))
            negativeButton.setBackgroundResource(R.drawable.outlined_button_background)
            negativeButton.setPadding(32, 16, 32, 16)
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

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.mycolor))
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            positiveButton.setPadding(32, 16, 32, 16)
            positiveButton.setOnClickListener {
                positiveAction()
                dialog.dismiss()
            }
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.let { negativeButton ->
            negativeButton.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent))
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.black))
            negativeButton.setBackgroundResource(R.drawable.outlined_button_background)
            negativeButton.setPadding(32, 16, 32, 16)
            negativeButton.setOnClickListener {
                negativeAction()
                dialog.dismiss()
            }
        }
    }
}