package com.bankledger.safecold.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.bankledger.safecold.R;

import java.util.ArrayList;
import java.util.List;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/11/7 16:46
 */
public class CommonLevelBar extends View {
    private final Context context;
    private float explainPadding;
    private int explainTextColor;
    private float explainTextSize;

    private int defaultColor;
    private int barColor;
    private float barSize;
    private float levelRadius;
    private int maxLevel;
    private final Paint sPaint;
    private final Paint dPaint;
    private final Paint tPaint;


    private int viewWidth;
    private int viewHeight;
    private float gap;

    private List<PointF> levelPoints;
    private int level = 2;

    private List<String> explains;
    private float tWidth;//第一个和最后一个level预留出的文字宽度

    public CommonLevelBar(Context context) {
        this(context, null);
    }

    public CommonLevelBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommonLevelBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CommonLevelBar);
        defaultColor = ta.getColor(R.styleable.CommonLevelBar_defaultColor, Color.parseColor("#F5F5F5"));
        barColor = ta.getColor(R.styleable.CommonLevelBar_barColor, context.getColor(R.color.colorPrimaryDark));
        barSize = ta.getDimension(R.styleable.CommonLevelBar_barSize, dp2px(8));
        levelRadius = ta.getDimension(R.styleable.CommonLevelBar_levelRadius, dp2px(8));
        maxLevel = ta.getInteger(R.styleable.CommonLevelBar_maxLevel, 4);
        levelPoints = new ArrayList<>(maxLevel);
        explains = new ArrayList<>(maxLevel);
        setExplains(null);
        explainTextSize = ta.getDimension(R.styleable.CommonLevelBar_explainTextSize, sp2px(14));
        explainTextColor = ta.getColor(R.styleable.CommonLevelBar_explainTextColor, context.getColor(R.color.colorPrimaryDark));
        explainPadding = ta.getDimension(R.styleable.CommonLevelBar_explainPadding, dp2px(2));

        dPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dPaint.setColor(defaultColor);
        dPaint.setStyle(Paint.Style.FILL);
        sPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sPaint.setColor(barColor);
        sPaint.setStyle(Paint.Style.FILL);
        tPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tPaint.setColor(explainTextColor);
        tPaint.setTextSize(explainTextSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLevel(canvas, dPaint, maxLevel);
        drawLevel(canvas, sPaint, level);
        drawExplain(canvas);
    }

    private void drawExplain(Canvas canvas) {
        for (int i = 0; i < explains.size(); i++) {
            float explainWidth = tPaint.measureText(explains.get(i));
            float explainBottom = tPaint.getFontMetrics().bottom;
            canvas.drawText(explains.get(i), levelPoints.get(i).x - explainWidth / 2, viewHeight - explainPadding - explainBottom, tPaint);
        }

    }

    private void drawLevel(Canvas canvas, Paint paint, int level) {
        RectF rect = new RectF(levelRadius + tWidth / 2, levelPoints.get(0).y - barSize / 2, levelPoints.get(level - 1).x, levelPoints.get(0).y + barSize / 2);
        canvas.drawRect(rect, paint);
        for (int i = 0; i < level; i++) {
            canvas.drawCircle(levelPoints.get(i).x, levelPoints.get(i).y, levelRadius, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        calculateLevelPosition();
    }

    //计算圆圈位置
    private void calculateLevelPosition() {
        tWidth = Math.max(tPaint.measureText(explains.get(0)), tPaint.measureText(explains.get(explains.size() - 1)));
        float temp = Math.max(2 * levelRadius, tWidth + explainPadding * 2);
        float realWidth = viewWidth - temp;
        gap = realWidth / (maxLevel - 1);
        float y = (viewHeight - explainTextSize) / 2 - explainPadding;
        for (int i = 0; i < maxLevel; i++) {
            levelPoints.add(new PointF(temp / 2 + gap * i, y));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (event.getY() >= 0 && event.getY() <= viewHeight) {
                    for (int i = 0; i < levelPoints.size(); i++) {
                        if (Math.abs(levelPoints.get(i).x - event.getX()) < gap / 2) {
                            level = i + 1;
                            break;
                        }
                    }
                    postInvalidate();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int currentLevel) {
        this.level = currentLevel;
        postInvalidate();
    }

    public void setDefaultColor(int defaultColor) {
        this.defaultColor = defaultColor;
        postInvalidate();
    }

    public void setBarColor(int barColor) {
        this.barColor = barColor;
        postInvalidate();
    }

    public void setBarSize(float barSize) {
        this.barSize = barSize;
        postInvalidate();
    }

    public void setLevelRadius(float levelRadius) {
        this.levelRadius = levelRadius;
        postInvalidate();
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        postInvalidate();
    }

    public void setExplains(List<String> explains) {
        this.explains.clear();
        for (int i = 0; i < maxLevel; i++) {
            this.explains.add("");
        }

        if (explains != null) {
            int len = explains.size() > maxLevel ? maxLevel : explains.size();
            for (int i = 0; i < len; i++) {
                this.explains.set(i, explains.get(i));
            }
        }
        postInvalidate();
    }

    public void notExplains() {
        setExplainTextSize(0);
        setExplainPadding(0);
    }

    public void setExplainTextColor(int explainTextColor) {
        this.explainTextColor = explainTextColor;
        postInvalidate();
    }

    public void setExplainTextSize(float explainTextSize) {
        this.explainTextSize = dp2px(explainTextSize);
        postInvalidate();
    }

    public void setExplainPadding(float explainPadding) {
        this.explainPadding = explainPadding;
        postInvalidate();
    }

    private int dp2px(float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int sp2px(int spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
