// FloatingActionButtonHelper.java
package com.example.melissa;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FloatingActionButtonHelper {

    private final FloatingActionButton fab;

    public FloatingActionButtonHelper(Context context) {
        // FloatingActionButton 생성
        fab = new FloatingActionButton(context);
        fab.setImageResource(android.R.drawable.ic_input_add); // 기본 아이콘 설정
        fab.setSize(FloatingActionButton.SIZE_NORMAL); // 버튼 크기 설정
        fab.setBackgroundColor(Color.parseColor("#FF6200EE")); // 배경 색상 설정

        // 위치 및 레이아웃 파라미터 설정
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; // 중앙 하단 정렬
        params.bottomMargin = 50; // 하단 여백 설정
        fab.setLayoutParams(params);

        // 클릭 이벤트 설정
        fab.setOnClickListener(v -> performScaleAnimation());
    }

    public FloatingActionButton getFab() {
        return fab;
    }

    // 애니메이션 설정 메서드
    private void performScaleAnimation() {
        // 확대 및 축소 애니메이션
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(fab, "scaleX", 1.0f, 1.5f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(fab, "scaleY", 1.0f, 1.5f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(fab, "scaleX", 1.5f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(fab, "scaleY", 1.5f, 1.0f);

        // 애니메이션 세트 구성
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
