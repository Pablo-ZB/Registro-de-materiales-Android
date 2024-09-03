package com.example.inventariodaws.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import com.example.inventariodaws.R
import kotlin.math.roundToInt

class BtnLoadingProgressBar(view: View) {
    private val layout = view as LinearLayout
    val progressbar: ProgressBar = view.findViewById(R.id.btn_progressbar)
    private val textView: TextView = view.findViewById(R.id.btn_textview)
    val ivDone: ImageView = view.findViewById(R.id.btn_image_check)

    fun setLoading() {
        layout.isEnabled = false
        textView.visibility = View.GONE
        progressbar.visibility = View.VISIBLE
        ivDone.visibility = View.GONE
        progressbar.scaleX = 1f
        ivDone.scaleX = 0f
    }

    fun setState(isSuccess: Boolean, onAnimationEnd: () -> Unit) {
        val v: Drawable = layout.background
        val bgColor: Int = if (isSuccess)
            Color.parseColor("#1ABE57")
        else
            Color.parseColor("#FF4343")

        val bgAnim: ValueAnimator = ObjectAnimator.ofFloat(0f, 1f).setDuration(600L)
        bgAnim.addUpdateListener {
            val mul: Float = it.animatedValue as Float
            v.colorFilter =
                PorterDuffColorFilter(adjustAlpha(bgColor, mul), PorterDuff.Mode.SRC_ATOP)
        }
        bgAnim.start()
        bgAnim.doOnEnd {
            if (isSuccess)
                flipProgressBar(
                    R.drawable.check_icon,
                    R.drawable.btn_success_bg
                ) { if (it) onAnimationEnd() }
            else
                flipProgressBar(
                    R.drawable.error_icon,
                    R.drawable.btn_error_bg
                ) { if (it) onAnimationEnd() }
        }
    }

    private fun flipProgressBar(
        img: Int,
        imageBackgroundDrawable: Int,
        isEnded: (Boolean) -> Unit
    ) {
        ivDone.setBackgroundResource(imageBackgroundDrawable)
        ivDone.setImageResource(img)

        val flip1: ObjectAnimator = ObjectAnimator.ofFloat(progressbar, "scaleX", 1f, 0f)
        val flip2: ObjectAnimator = ObjectAnimator.ofFloat(ivDone, "scaleX", 0f, 1f)
        flip1.duration = 400
        flip2.duration = 400
        flip1.interpolator = DecelerateInterpolator()
        flip2.interpolator = AccelerateDecelerateInterpolator()
        flip1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                progressbar.visibility = View.GONE
                ivDone.visibility = View.VISIBLE
                flip2.start()
            }
        })
        flip1.start()
        flip2.doOnEnd { isEnded(true) }
    }

    fun reset() {
        textView.visibility = View.VISIBLE
        progressbar.visibility = View.GONE
        ivDone.visibility = View.GONE
        progressbar.scaleX = 1f
        ivDone.scaleX = 0f
        layout.background.clearColorFilter()
        layout.isEnabled = true
    }

    private fun adjustAlpha(bgColor: Int, mul: Float): Int {
        val a: Int = (Color.alpha(bgColor) * mul).roundToInt()
        val r: Int = Color.red(bgColor)
        val g: Int = Color.green(bgColor)
        val b: Int = Color.blue(bgColor)

        return Color.argb(a, r, g, b)
    }
}