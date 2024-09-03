@file:Suppress("DEPRECATION")

package com.example.inventariodaws.util

import android.app.Activity
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.inventariodaws.R

fun Toast.showCustomToast(message: String, activity: Activity, iconResId: Int, severity: String, toastDuration: Int)
{
    val layout = activity.layoutInflater.inflate (
        R.layout.custom_toast,
        activity.findViewById(R.id.custom_toast_container)
    )

    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = message

    val imageView = layout.findViewById<ImageView>(R.id.toast_icon)
    imageView.setImageResource(iconResId)

    val container = layout.findViewById<LinearLayout>(R.id.custom_toast_container)
    val backgroundResId = when (severity) {
        "error" -> R.drawable.toast_error
        "warning" -> R.drawable.toast_warning
        "info" -> R.drawable.toast_info
        else -> R.drawable.toast_success
    }
    container.setBackgroundResource(backgroundResId)

    this.apply {
        setGravity(Gravity.BOTTOM, 0, 70)
        duration = toastDuration
        view = layout
        show()
    }
}