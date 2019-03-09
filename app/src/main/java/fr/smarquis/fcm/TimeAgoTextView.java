package fr.smarquis.fcm;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class TimeAgoTextView extends AppCompatTextView {

    public static final long NO_TIMESTAMP = 0;

    private static final long SECOND_TO_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private static final long MINUTE_TO_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private static final long HOUR_TO_MILLIS = TimeUnit.HOURS.toMillis(1);

    private long timestamp;

    @Nullable
    private CountDownTimer countDownTimer;

    private boolean isAttachedToWindow = false;

    public TimeAgoTextView(Context context) {
        super(context);
    }

    public TimeAgoTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeAgoTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        renderTimestamp();
        restartCountDown();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
        startCountDown();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        stopCountDown();
    }

    private void startCountDown() {
        if (isAttachedToWindow && timestamp > NO_TIMESTAMP) {
            if (countDownTimer == null) {
                final long now = System.currentTimeMillis();
                final long diff = Math.abs(now - timestamp);

                long millisInFuture;
                long countDownInterval;
                if (diff < MINUTE_TO_MILLIS) {
                    millisInFuture = MINUTE_TO_MILLIS;
                    countDownInterval = SECOND_TO_MILLIS;
                } else if (diff < HOUR_TO_MILLIS) {
                    millisInFuture = HOUR_TO_MILLIS;
                    countDownInterval = MINUTE_TO_MILLIS;
                } else {
                    // ignore
                    return;
                }
                countDownTimer = new CountDownTimer(millisInFuture, countDownInterval) {

                    public void onTick(long millisUntilFinished) {
                        renderTimestamp();
                    }

                    public void onFinish() {
                        restartCountDown();
                    }
                }.start();
            }
        }
    }

    private void restartCountDown() {
        stopCountDown();
        startCountDown();
    }

    private void stopCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void renderTimestamp() {
        if (timestamp > NO_TIMESTAMP) {
            final long now = System.currentTimeMillis();
            setText(String.valueOf(DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)));
        } else {
            setText(null);
        }
    }


}