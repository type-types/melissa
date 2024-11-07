// FloatingActionButtonHelper.java
package com.example.melissa;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FloatingActionButtonHelper {

    private final FloatingActionButton fab;
    private final Context context; // Context를 받아옴

    public FloatingActionButtonHelper(Context context) {
        this.context = context; // Context 초기화
        fab = new FloatingActionButton(context);
        fab.setImageResource(android.R.drawable.ic_input_add);
        fab.setSize(FloatingActionButton.SIZE_NORMAL);
        fab.setBackgroundColor(Color.parseColor("#FF6200EE"));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = 20;
        fab.setLayoutParams(params);

        fab.setOnClickListener(v -> {
            performScaleAnimation(); // 애니메이션 수행
            context.startActivity(new Intent(context, ChatActivity.class)); // ChatActivity로 이동
        });
    }

    public FloatingActionButton getFab() {
        return fab;
    }

    private void performScaleAnimation() {
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(fab, "scaleX", 1.0f, 1.5f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(fab, "scaleY", 1.0f, 1.5f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(fab, "scaleX", 1.5f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(fab, "scaleY", 1.5f, 1.0f);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.play(scaleUpX).with(scaleUpY);
        scaleUp.setDuration(150);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);
        scaleDown.setDuration(150);

        AnimatorSet animationSet = new AnimatorSet();
        animationSet.play(scaleUp).before(scaleDown);
        animationSet.start();
    }
}
