package com.swing.contacts.common

import android.content.Context
import android.widget.Toast

class ViewUtils {
    companion object {
        fun showToast(context: Context, msg: String) {
            Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
        }
    }
}