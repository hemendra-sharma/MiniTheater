package com.hemendra.minitheater.view

import android.app.AlertDialog
import android.content.Context

fun showMessage(context: Context, msg: String, onOkClicked: Runnable? = null) {
    val dialog: AlertDialog = AlertDialog.Builder(context).setCancelable(true)
            .setMessage(msg)
            .setPositiveButton("OK") { _, _ -> onOkClicked?.run() }.create()
    dialog.show()
}

fun showYesNoMessage(context: Context, msg: String,
                     onYesClicked: Runnable?, onNoClicked: Runnable? = null) {
    val dialog: AlertDialog = AlertDialog.Builder(context).setCancelable(true)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> onYesClicked?.run() }
            .setNegativeButton("No") { _, _ -> onNoClicked?.run() }.create()
    dialog.show()
}

fun showCustomMessage(context: Context, msg: String,
                      positiveText: String, negativeText: String,
                      onPositiveClicked: Runnable?,
                      onNegativeClicked: Runnable? = null) {
    val dialog: AlertDialog = AlertDialog.Builder(context).setCancelable(true)
            .setMessage(msg)
            .setPositiveButton(positiveText) { _, _ -> onPositiveClicked?.run() }
            .setNegativeButton(negativeText) { _, _ -> onNegativeClicked?.run() }.create()
    dialog.show()
}