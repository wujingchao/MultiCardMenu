package net.wujingchao.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * @author wujingchao  2015-04-18 email:wujingchao@aliyun.com
 */
class DarkFrameLayout extends FrameLayout {

    public final static int MAX_ALPHA = 0x7f;

    private Paint mFadePaint;

    private int alpha = 0x00;

    private MultiCardMenu multiMenu;

    public DarkFrameLayout(Context context) {
        this(context, null);
    }

    public DarkFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public DarkFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFadePaint = new Paint();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return multiMenu.isDisplaying();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        drawFade(canvas);
    }

    private void drawFade(Canvas canvas) {
        mFadePaint.setColor(Color.argb(alpha, 0, 0, 0));
        canvas.drawRect(0, 0, getMeasuredWidth(), getHeight(), mFadePaint);
    }

    public void fade(boolean fade) {
        this.alpha = fade ? 0x8f : 0x00;
        invalidate();
    }

    public void fade(int alpha) {
        this.alpha = alpha;
        invalidate();
    }

    public void setMultiMenu(MultiCardMenu multiMenu) {
        this.multiMenu = multiMenu;
    }

}
