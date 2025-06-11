package com.demo.map.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.demo.map.R;
import com.demo.map.model.Achievement;

public class AchievementToast {
    private static final long DISPLAY_DURATION = 3000; // 3 seconds
    private static final long ANIMATION_DURATION = 500; // 0.5 seconds

    public static void show(Context context, Achievement achievement) {
        ViewGroup rootView = ((ViewGroup) ((android.app.Activity) context)
            .getWindow().getDecorView().findViewById(android.R.id.content));

        View toastView = LayoutInflater.from(context).inflate(R.layout.achievement_toast, rootView, false);
        
        TextView titleText = toastView.findViewById(R.id.achievementTitle);
        TextView descriptionText = toastView.findViewById(R.id.achievementDescription);
        TextView pointsText = toastView.findViewById(R.id.achievementPoints);
        CardView cardView = toastView.findViewById(R.id.achievementCard);

        titleText.setText(achievement.getType().getTitle());
        descriptionText.setText(achievement.getType().getDescription());
        pointsText.setText(String.format("+%d", achievement.getType().getPoints()));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = 100;
        toastView.setLayoutParams(params);

        rootView.addView(toastView);

        // Slide in from top
        toastView.setTranslationY(-toastView.getHeight());
        toastView.setAlpha(0f);
        toastView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Wait for display duration then slide out
                    toastView.postDelayed(() -> {
                        toastView.animate()
                            .translationY(-toastView.getHeight())
                            .alpha(0f)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    rootView.removeView(toastView);
                                }
                            });
                    }, DISPLAY_DURATION);
                }
            });
    }
} 