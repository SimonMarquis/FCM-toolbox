package fr.smarquis.fcm.view.ui

import android.content.Context
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.concurrent.TimeUnit.*
import kotlin.math.abs

class TimeAgoTextView
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var timestamp: Long = NO_TIMESTAMP
        set(value) {
            field = value
            renderTimestamp()
            restartCountDown()
        }
    private var countDownTimer: CountDownTimer? = null
    private var isCounting = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isCounting = true
        startCountDown()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isCounting = false
        stopCountDown()
    }

    private fun startCountDown() {
        if (!isCounting || timestamp <= NO_TIMESTAMP || countDownTimer != null) return
        val now = System.currentTimeMillis()
        val diff = abs(now - timestamp)
        val millisInFuture: Long
        val countDownInterval: Long
        when {
            diff < MINUTE_TO_MILLIS -> {
                millisInFuture = MINUTE_TO_MILLIS
                countDownInterval = SECOND_TO_MILLIS
            }
            diff < HOUR_TO_MILLIS -> {
                millisInFuture = HOUR_TO_MILLIS
                countDownInterval = MINUTE_TO_MILLIS
            }
            else -> /*ignore*/ return
        }
        countDownTimer = object : CountDownTimer(millisInFuture, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) = renderTimestamp()
            override fun onFinish() = restartCountDown()
        }.start()
    }

    private fun restartCountDown() {
        stopCountDown()
        startCountDown()
    }

    private fun stopCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun renderTimestamp() {
        text = if (timestamp > NO_TIMESTAMP)
            DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), SECOND_IN_MILLIS, FORMAT_ABBREV_RELATIVE)
        else
            null
    }

    companion object {
        const val NO_TIMESTAMP: Long = 0
        private val SECOND_TO_MILLIS = SECONDS.toMillis(1)
        private val MINUTE_TO_MILLIS = MINUTES.toMillis(1)
        private val HOUR_TO_MILLIS = HOURS.toMillis(1)
    }
}