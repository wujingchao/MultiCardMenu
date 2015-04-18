package net.wujingchao.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull; 
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wujingchao  2015-04-01 email:wujingchao@aliyun.com
 */
public class MultiCardMenu extends FrameLayout {

    private final static String TAG = "MultiCardMenu";

    private final static int DEFAULT_CARD_MARGIN_TOP = 0;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD = 60;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD = 20;

    private final static int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 30;

    private final static int DEFAULT_DURATION = 300;

    private float mDensity;

    private float mTitleBarHeightOnSpread;

    private float mTitleBarHeightOnFold;

    private VelocityTracker mVelocityTracker;

    private float mMarginTop;

    private float deltaY;

    private int whichCardOnTouch = -1;

    private float downY;

    private float firstDownY;

    private float firstDownX;

    private boolean isTouchOnCard = false;

    private int mChildCount;

    private boolean isDisplaying = false;

    private Interpolator mInterpolator = new AccelerateInterpolator();

    private float mMoveDistanceToTrigger;

    private int mDisplayingCard  = -1;

    private int mMaxVelocity;

    private int mMinVelocity;

    private boolean isDragging = false;

    private float xVelocity;

    private float yVelocity;

    private OnDisplayOrHideListener mOnDisplayOrHideListener;

    private boolean isExistBackground = false;

    private int mDuration;

    private Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            isAnimating = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            isAnimating = false;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            isAnimating = false;
        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };

    private boolean isAnimating = false;

    public MultiCardMenu(Context context) {
        this(context,null);
    }

    public MultiCardMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MultiCardMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiCardMenu, defStyleAttr, 0);
        mTitleBarHeightOnSpread = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_spread,dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD));
        mTitleBarHeightOnFold = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_fold, dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD));
        mMarginTop = a.getDimension(R.styleable.MultiCardMenu_margin_top, dip2px(DEFAULT_CARD_MARGIN_TOP));
        mMoveDistanceToTrigger = a.getDimension(R.styleable.MultiCardMenu_move_distance_to_trigger,dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));
        int mBackgroundRid = a.getResourceId(R.styleable.MultiCardMenu_background_layout,-1);
        if(mBackgroundRid != -1) {
            LayoutInflater.from(context).inflate(mBackgroundRid,this);
            isExistBackground = true;
        }
        mDuration = a.getInt(R.styleable.MultiCardMenu_animator_duration,DEFAULT_DURATION);
        a.recycle();
        ViewConfiguration vc = ViewConfiguration.get(context);
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity() * 2;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mChildCount = getChildCount();
        for(int i = 0; i < mChildCount; i ++) {
            View childView = getChildAt(i);
            if(i == 0 && isExistBackground) {
                childView.layout(0,0,childView.getMeasuredWidth(),childView.getMeasuredHeight());
                continue;
            }
            //l t r b
            int t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);
        }
    }

    private void initVelocityTracker(MotionEvent event) {
        if(mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if(mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        initVelocityTracker(event);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                releaseVelocityTracker();
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
            hideCard(mDisplayingCard);
        }else if(isDisplaying && downY > mMarginTop && downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
        }

        if(isExistBackground && whichCardOnTouch == 0){
            isTouchOnCard = false;
        }
    }

    private void handleActionMove(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard)return;
        computeVelocity();
        if(Math.abs(yVelocity) < Math.abs(xVelocity)) return;
        event.setAction(MotionEvent.ACTION_CANCEL | (event.getActionIndex()<< MotionEvent.ACTION_POINTER_INDEX_SHIFT));
        isDragging = true;
        deltaY = event.getY() - downY;
        downY = event.getY();
        View touchingChildView = getChildAt(whichCardOnTouch);
        touchingChildView.offsetTopAndBottom((int) deltaY);
    }

    private void handleActionUp(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard) return;
        computeVelocity();
        if(!isDisplaying && (
            ((int)event.getY() == (int)firstDownY && (int)event.getX() == (int)firstDownX) //means click...
            || (event.getY() - firstDownY < 0 && (Math.abs(event.getY() - firstDownY) > mMoveDistanceToTrigger))
            || (yVelocity < 0 && Math.abs(yVelocity) > mMinVelocity && Math.abs(yVelocity)> Math.abs(xVelocity))
        )) {
            displayCard(whichCardOnTouch);
        }else if(!isDisplaying && ((event.getY() - firstDownY > 0) || Math.abs(event.getY() - firstDownY) < mMoveDistanceToTrigger)) {
            hideCard(whichCardOnTouch);
        }else if(isDisplaying) {
            float currentY = ViewHelper.getY(getChildAt(mDisplayingCard));
            if(currentY < mMarginTop || currentY < (mMarginTop + mMoveDistanceToTrigger)) {
                ObjectAnimator.ofFloat(getChildAt(mDisplayingCard),"y",
                        currentY,mMarginTop)
                        .setDuration(mDuration)
                        .start();
            }else if(currentY > (mMarginTop + mMoveDistanceToTrigger)) {
                hideCard(mDisplayingCard);
            }
        }
        isTouchOnCard = false;
        deltaY = 0;
        isDragging = false;
    }

    private void computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000,mMaxVelocity);
        yVelocity = mVelocityTracker.getYVelocity();
        xVelocity = mVelocityTracker.getXVelocity();
    }

    private void displayCard(final int which) {
        //TODO add fade
        if(isDisplaying || isAnimating)return;
        List<Animator> animators = new ArrayList<>(mChildCount);
        ObjectAnimator displayAnimator = ObjectAnimator
                .ofFloat(getChildAt(which), "y", ViewHelper.getY(getChildAt(which)), mMarginTop)
                .setDuration(mDuration);
//        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(getChildAt(which)), mMarginTop)
//                 .setDuration(mDuration);
//        displayAnimator.setTarget(getChildAt(which));
//        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                float value = (float) valueAnimator.getAnimatedValue();
//                ViewHelper.setY(getChildAt(which),value);
//            }
//        });
        animators.add(displayAnimator);
        int n = isExistBackground ? (mChildCount - 1) : mChildCount;
        for(int i = 0,j = 1; i < mChildCount; i++) {
            if(i == 0 && isExistBackground) continue;
            if(i != which){
                animators.add(ObjectAnimator
                        .ofFloat(getChildAt(i),"y",ViewHelper.getY(getChildAt(i)),
                                getMeasuredHeight() - mTitleBarHeightOnFold * (n - j))
                        .setDuration(mDuration));
                j ++;
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(mAnimatorListener);
        set.setInterpolator(mInterpolator);
        set.playTogether(animators);
        set.start();
        isDisplaying = true;
        mDisplayingCard = which;
        if(mOnDisplayOrHideListener != null)
            mOnDisplayOrHideListener.onDisplay(isExistBackground ? (which - 1) : which);
    }


    private void hideCard(int which) {
        if(isAnimating)return;
        List<Animator> animators = new ArrayList<>(mChildCount);
        final View displayingCard = getChildAt(which);
        int t = (int) (getMeasuredHeight() - (mChildCount - which)* mTitleBarHeightOnSpread);
        ObjectAnimator displayAnimator = ObjectAnimator.ofFloat(displayingCard, "y",
                ViewHelper.getY(displayingCard), t).setDuration(mDuration);
//        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(displayingCard), t)
//                       .setDuration(mDuration);
//        displayAnimator.setTarget(displayingCard);
//        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                float value = (float) valueAnimator.getAnimatedValue();
//                ViewHelper.setY(displayingCard,value);
//            }
//        });
        animators.add(displayAnimator);
        for(int i = 0; i < mChildCount; i ++) {
            if(i == 0 && isExistBackground) continue;
            if(i != which) {
                t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
                animators.add(ObjectAnimator.ofFloat(getChildAt(i),"y",
                        ViewHelper.getY(getChildAt(i)),t));
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(mAnimatorListener);
        set.setInterpolator(mInterpolator);
        set.playTogether(animators);
        set.start();
        isDisplaying = false;
        mDisplayingCard = -1;
        if(mOnDisplayOrHideListener != null)
            mOnDisplayOrHideListener.onHide(isExistBackground ? (which - 1) : which);
    }


    public void setOnDisplayOrHideListener(OnDisplayOrHideListener onDisplayOrHideListener) {
        this.mOnDisplayOrHideListener = onDisplayOrHideListener;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isDragging;
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


    public interface OnDisplayOrHideListener {

        public void onDisplay(int which);

        public void onHide(int which);

    }

}
