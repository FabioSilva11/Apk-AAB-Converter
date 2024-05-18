package com.fabiosilva.packconvert.activity

import android.app.ProgressDialog
import android.content.Context

class CustomProgressDialog(private val context: Context) {

    private var progressDialog: ProgressDialog? = null

    fun show(message: String) {
        dismiss() // Garante que qualquer diálogo anterior seja descartado antes de mostrar o novo.

        progressDialog = ProgressDialog(context)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    fun dismiss() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        progressDialog = null
    }
}
