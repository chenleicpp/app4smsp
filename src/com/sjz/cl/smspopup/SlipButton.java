package com.sjz.cl.smspopup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class SlipButton extends TextView implements OnTouchListener {

	public interface OnChangedListener {
		abstract void OnChanged(String strName, boolean CheckState);
	}

	private String strName;
	private boolean enabled = true;
	public boolean flag = false;// 设置初始化状态
	public boolean SlipState = false;// 记录当前按钮是否打开,true为打开,flase为关闭
	private boolean OnSlip = false;// 记录用户是否在滑动的变量
	public float DownX = 0f, NowX = 0f;// 按下时的x,当前的x,NowX>100时为ON背景,反之为OFF背景
	private Rect Slipbtn_on_back, Slipbtn_on_front; // 圆圈按钮的位置在前面或后面

	private int SlipButtonWidth;
	private int SlipButtonHeight;

	private boolean isChgLsnOn = false;
	private OnChangedListener ChgLsn;
	private Bitmap bg_on, bg_off, slip_btn;

	public SlipButton(Context context) {
		super(context);
		init();
	}

	public SlipButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void setChecked(boolean b) {
		if (b) {
			flag = true;
			SlipState = true;
			NowX = 80;
		} else {
			flag = false;
			SlipState = false;
			NowX = 0;
		}
	}

	public void setEnabled(boolean b) {
		if (b) {
			enabled = true;
		} else {
			enabled = false;
		}
	}

	private void init() {
		bg_on = BitmapFactory.decodeResource(getResources(), R.drawable.on_btn);
		bg_off = BitmapFactory.decodeResource(getResources(), R.drawable.off_btn);
		slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.white_btn);

		SlipButtonWidth = bg_off.getWidth();
		SlipButtonHeight = slip_btn.getHeight();

		Slipbtn_on_front = new Rect(0, 0, slip_btn.getWidth(), slip_btn.getHeight());
		Slipbtn_on_back = new Rect(bg_off.getWidth() - slip_btn.getWidth(), 0, bg_off.getWidth(), slip_btn.getHeight());
		setOnTouchListener(this);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasureWidth(widthMeasureSpec);
		int height = getMeasuredHeight(heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	private int getMeasureWidth(int widthMeasureSpec) {
		int result = 100;
		int specMode = MeasureSpec.getMode(widthMeasureSpec);
		int specSize = MeasureSpec.getSize(widthMeasureSpec);

		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = bg_off.getWidth();
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			result = bg_off.getWidth();
			break;
		}
		return result;
	}

	private int getMeasuredHeight(int heightMeasureSpec) {
		int result = 100;
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);

		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			result = slip_btn.getHeight();
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			result = slip_btn.getHeight();
			break;
		}
		return result;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Matrix matrix = new Matrix();
		Paint paint = new Paint();
		float x;
		{
			if (flag) {
				NowX = 80;
				flag = false;
			}
			if (NowX < (bg_on.getWidth() / 2))
				canvas.drawBitmap(bg_off, matrix, paint);
			else
				canvas.drawBitmap(bg_on, matrix, paint);

			if (OnSlip) {// 滑动状态
				if (NowX >= bg_on.getWidth())
					x = bg_on.getWidth() - slip_btn.getWidth() / 2;// 减去游标1/2的长度...
				else
					x = NowX - slip_btn.getWidth() / 2;
			} else {// 非滑动状态
				if (SlipState)// 打开状态时候圆圈在后面
					x = Slipbtn_on_back.left;
				else
					// 关闭状态时候圆圈在前面
					x = Slipbtn_on_front.left;
			}
			// 异常判断情况
			if (x < 0)
				x = 0;
			else if (x > bg_off.getWidth() - slip_btn.getWidth())
				x = bg_off.getWidth() - slip_btn.getWidth();
			canvas.drawBitmap(slip_btn, x, 0, paint);// 画出圆圈按钮.
		}
	}

	public void SetOnChangedListener(String name, OnChangedListener l) {// 设置监听器,当状态修改的时候
		strName = name;
		isChgLsnOn = true;
		ChgLsn = l;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled) {
			return false;
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			NowX = event.getX();
			break;
		case MotionEvent.ACTION_DOWN:
			if (event.getX() > bg_on.getWidth() || event.getY() > bg_on.getHeight())
				return false;
			OnSlip = true;
			DownX = event.getX();
			NowX = DownX;
			break;
		case MotionEvent.ACTION_UP:// 松开
			OnSlip = false;
			boolean LastChoose = SlipState;
			if (event.getX() >= (bg_on.getWidth() / 2))
				SlipState = true;
			else
				SlipState = false;
			if (isChgLsnOn && (LastChoose != SlipState))// 如果设置了监听器,就调用其方法..
				ChgLsn.OnChanged(strName, SlipState);
			break;
		default:

		}
		invalidate();// 重画控件
		return true;
	}

}
