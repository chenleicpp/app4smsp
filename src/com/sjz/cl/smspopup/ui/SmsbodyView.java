package com.sjz.cl.smspopup.ui;

import com.sjz.cl.smspopup.R;
import com.sjz.cl.smspopup.utils.MessageObject;
import com.sjz.cl.smspopup.utils.PopupUtil;
import com.sjz.cl.smspopup.utils.PopupUtil.ContactIdentification;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * For this class is for swiper view ,this class contain except EditText and
 * Reply Button
 * 
 * @author lei.chen
 * 
 */
public class SmsbodyView extends LinearLayout {

    private OnButtonClicked mListener = null;
    private MessageObject message;
    private Context context;
    private ImageView mDeleteBtn;
    private ImageView mCloseBtn;
    private TextView mContactName;
    private TextView mTimestamp;
    private TextView mBody;
    private ScrollView mScrollView;

    public SmsbodyView(Context _context, MessageObject message) {
        super(_context);
        context = _context;
        setupLayout(context);
        InvalidateViews(message);
    }

    public SmsbodyView(Context _context, AttributeSet attrs) {
        super(_context, attrs);
        context = _context;
        setupLayout(context);
    }

    private void setupLayout(Context context) {
        View.inflate(context, R.layout.smspopup_swiper, this);

        mScrollView = (ScrollView) this.findViewById(R.id.popup_scrollview);
        mScrollView.setVerticalFadingEdgeEnabled(false);
        mDeleteBtn = (ImageView) findViewById(R.id.popup_del_btn);
        mCloseBtn = (ImageView) findViewById(R.id.popup_close_btn);
        mContactName = (TextView) this.findViewById(R.id.popup_name_tv);
        mTimestamp = (TextView) this.findViewById(R.id.popup_time_tv);
        mBody = (TextView) this.findViewById(R.id.popup_content_tv);

        mCloseBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.closeBtnClicked();
                }
            }
        });

        mDeleteBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.deleteBtnClicked();
                }
            }
        });

        mBody.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (mListener != null) {
                        mListener.bodyTvClicked();
                    }
                    break;
                }
                return true;
            }
        });

        mContactName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.contactTvClicked();
                }
            }
        });
    }

    private void InvalidateViews(MessageObject message) {

        String addr = message.getFromAddress();
        String personName = "";
        ContactIdentification ci = PopupUtil.getPersonIdFromPhoneNumber(
                context, addr);
        if (ci != null) {
            personName = ci.getContactName();
        } else {
            personName = addr;
        }
        mContactName.setText(personName);
        mBody.setText(message.getMessageBody());
        String time = PopupUtil
                .formatTimestamp(context, message.getTimestamp());
        mTimestamp.setText(time);
    }

    public void setOnButtonClicked(OnButtonClicked listener) {
        mListener = listener;
    }

    public static interface OnButtonClicked {
        abstract void closeBtnClicked();

        abstract void deleteBtnClicked();

        abstract void contactTvClicked();

        abstract void bodyTvClicked();
    }

}
