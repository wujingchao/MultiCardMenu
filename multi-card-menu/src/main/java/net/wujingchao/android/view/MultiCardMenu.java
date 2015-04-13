package net.wujingchao.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * @author wujingchao  2015-04-01 email:wujingchao@aliyun.com
 */
public class MultiCardMenu extends FrameLayout {

    private final static String TAG = "MultiCardMenu";

    private final static int DEFAULT_CARD_MARGIN_TOP = 20;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD = 60;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD = 20;

    private final static int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 50;

    private final static int DEFAULT_DURATION = 300;

    private float mDensity;

    private float mTitleBarHeightOnSpread;

    private float mTitleBarHeightOnFold;

    public MultiCardMenu(Context context) {
        this(context,null);
    }

    public MultiCardMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    private float mMarginTop;

    private float deltaY;

    private int whichCardOnTouch = -1;

    private float downY;

    private float firstDownY;

    private float firstDownX;

    private boolean isTouchOnCard = false;

    private float mTouchViewOriginalY;

    private int mChildCount;

    private boolean isDisplaying = false;

    private Interpolator interpolator = new DecelerateInterpolator();

    private float mMoveDistanceToTrigger;

    private int mDisplayingCard  = -1;

    public MultiCardMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiCardMenu, defStyleAttr, 0);
        mTitleBarHeightOnSpread = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_spread,dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD));
        mTitleBarHeightOnFold = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_fold, dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD));
        mMarginTop = a.getDimension(R.styleable.MultiCardMenu_margin_top, dip2px(DEFAULT_CARD_MARGIN_TOP));
        mMoveDistanceToTrigger = a.getDimension(R.styleable.MultiCardMenu_move_distance_to_trigger,dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));
        a.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mChildCount = getChildCount();
        for(int i = 0; i < mChildCount; i ++) {
            View childView = getChildAt(i);
            //l t r b
            int t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleActionDown(MotionEvent event) {
        firstDownY = downY = event.getY();
        firstDownX = event.getX();
        //Judge which card on touching
        if(!isDisplaying && downY > (getMeasuredHeight() - mChildCount * mTitleBarHeightOnSpread)) {
            for(int i = 1; i <= mChildCount; i ++) {
                if(downY < (getMeasuredHeight() - mChildCount * mTitleBarHeightOnSpread
                        + mTitleBarHeightOnSpread * i)) {
                    whichCardOnTouch = i-1;
                    isTouchOnCard = true;
                    break;
                }
            }
        }else if(isDisplaying && downY > (getMeasuredHeight() - (mChildCount - 1) * mTitleBarHeightOnFold)) {
            Log.d(TAG,"click bottom card");
        }else if(isDisplaying && downY > mMarginTop && downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
            Log.d(TAG,"touch on display card");
        }
        if(whichCardOnTouch != -1)
            mTouchViewOriginalY = ViewHelper.getY(getChildAt(whichCardOnTouch));
    }

    private void handleActionMove(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard)return;
        View touchingChildView = getChildAt(whichCardOnTouch);
        deltaY = (event.getY() - downY);
        downY = event.getY();
        touchingChildView.offsetTopAndBottom((int) deltaY);
    }

    private void handleActionUp(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard) return;
        if(!isDisplaying && ((event.getY() == firstDownY && event.getX() == firstDownX) //means click...
                || (event.getY() - firstDownY < 0 && (Math.abs(event.getY() - firstDownY) > mMoveDistanceToTrigger)))) {
            displayCard(whichCardOnTouch);
        }else if(!isDisplaying && ((event.getY() - firstDownY > 0) || Math.abs(event.getY() - firstDownY) < mMoveDistanceToTrigger)) {
            hideCard(whichCardOnTouch);
        }else if(isDisplaying) {
            float currentY = ViewHelper.getY(getChildAt(mDisplayingCard));
            if(currentY < mMarginTop || currentY < (mMarginTop + mMoveDistanceToTrigger)) {
                ObjectAnimator.ofFloat(getChildAt(mDisplayingCard),"y",
                        currentY,mMarginTop)
                        .setDuration(DEFAULT_DURATION)
                        .start();
            }else if(currentY > (mMarginTop + mMoveDistanceToTrigger)) {
                hideCard(mDisplayingCard);
            }
        }
        isTouchOnCard = false;
        deltaY = 0;
    }

    private void displayCard(int which) {
        if(isDisplaying)return;
        ObjectAnimator [] animators = new ObjectAnimator[mChildCount];
        animators[0] = ObjectAnimator
                .ofFloat(getChildAt(which), "y", ViewHelper.getY(getChildAt(which)), mMarginTop)
                .setDuration(DEFAULT_DURATION);
        for(int i = 0,j=1; i < mChildCount; i++) {
            if(i != which){
                animators[j] = ObjectAnimator
                        .ofFloat(getChildAt(i),"y",ViewHelper.getY(getChildAt(i)),
                                getMeasuredHeight() - mTitleBarHeightOnFold * (mChildCount-j))
                        .setDuration(DEFAULT_DURATION);
                j ++;
            }
        }
//        for(ObjectAnimator o:animators) {
//            o.setInterpolator(interpolator);
//            o.start();
//        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.start();
        isDisplaying = true;
        mDisplayingCard = which;
    }

    private void hideCard(int which) {
        ObjectAnimator animators [] = new ObjectAnimator[mChildCount];
        View displayingCard = getChildAt(which);
        int t = (int) (getMeasuredHeight() - (mChildCount - which)* mTitleBarHeightOnSpread);
        animators[0] = ObjectAnimator.ofFloat(displayingCard,"y",
                ViewHelper.getY(displayingCard),t).setDuration(DEFAULT_DURATION);
        for(int i = 0,j = 1; i < mChildCount; i ++) {
            if(i != which) {
                t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
                animators[j] = ObjectAnimator.ofFloat(getChildAt(i),"y",
                        ViewHelper.getY(getChildAt(i)),t);
                j ++;
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.start();
//        for(ObjectAnimator o :animators) {
//            o.setInterpolator(interpolator);
//            o.start();
//        }
        isDisplaying = false;
        mDisplayingCard = -1;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    @SuppressWarnings("unused")
    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    private int dip2px(int dipVal) {
        return (int)(dipVal * mDensity + 0.5f);
    }
}
