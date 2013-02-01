package com.sjz.cl.smspopup.ui;

import java.util.List;
import com.sjz.cl.smspopup.R;
import com.sjz.cl.smspopup.receiver.ClearAllReceiver;
import com.sjz.cl.smspopup.ui.SmsPopupSwipeView.MessageCountChanged;
import com.sjz.cl.smspopup.ui.SmsbodyView.OnButtonClicked;
import com.sjz.cl.smspopup.utils.MessageObject;
import com.sjz.cl.smspopup.utils.PopupUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Intents;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * For this class is main activity to show popup window
 * @author chenlei
 *
 */
public class PopupActivity extends Activity {

    private static final String TAG = "popup.PopupActivity";

    private static final float WIDTH = 0.75f;

    private Bundle mBundle = null;

    // save messageobject may be use next
    private MessageObject mMsgObj;

    private InputMethodManager inputManager;

    private EditText mEditSend;
    private Button mReplyBtn;
    private TextView mPagerTv;

    private SmsPopupSwipeView mPopupSwipeView;

    private Resources res;
    
    private OnButtonClicked obc = new OnButtonClicked() {

        @Override
        public void deleteBtnClicked() {
            showDelAlert();
        }

        @Override
        public void closeBtnClicked() {
            MessageObject mo = mPopupSwipeView.getActiveMessage();
            mo.setMessageRead();
            removeActiveMessage();
        }

        @Override
        public void contactTvClicked() {
            MessageObject message = mPopupSwipeView.getActiveMessage();
            Uri createUri = Uri.fromParts("tel", (String)message.getFromAddress(), null);
            Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
            startActivity(intent);
        }

        @Override
        public void bodyTvClicked() {
            Intent i = PopupUtil.getSmsInboxIntent();
            getApplicationContext().startActivity(i);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.smspopup);

        res = getResources();

        // make the dialog width small
        LinearLayout mainLL = (LinearLayout) findViewById(R.id.MainLinearLayout);
        Display d = getWindowManager().getDefaultDisplay();
        int width = (int) (d.getWidth() * WIDTH);
        Log.v(TAG, "setting width to: " + width);
        mainLL.setMinimumWidth(width);

        // setup view
        setupView();

        if (mBundle == null) {
            invalidView(getIntent().getExtras());
        } else {
            invalidView(mBundle);
        }
    }

    private void setupView() {
        mPopupSwipeView = (SmsPopupSwipeView) this.findViewById(R.id.sms_popups_layout);
        mPagerTv = (TextView) this.findViewById(R.id.popup_pager_tv);
        mReplyBtn = (Button) this.findViewById(R.id.popup_reply_btn);
        mEditSend = (EditText) this.findViewById(R.id.popup_reply_et);
        mReplyBtn.setText(getResources().getString(R.string.popup_reply_btn));
        mReplyBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // quick reply sms
                quickReplyMessage(mEditSend.getText().toString());
                removeActiveMessage();
                mEditSend.setText("");
            }
        });
        mPopupSwipeView.setOnMessageCountChanged(new MessageCountChanged() {

            @Override
            public void onChange(int current, int total) {
                Log.d(TAG, "current page:" + current + 1 + "  total page:" + total);
                if (current == 0 && total == 1) {
                    mPagerTv.setVisibility(View.GONE);
                }else {
                    mPagerTv.setVisibility(View.VISIBLE);
                    mPagerTv.setText("(" + (current + 1) + "/" + total + ")");
                }
            }
        });
    }

    private void myFinish() {
        if (mPopupSwipeView.getChildCount() == 1) {
            finish();
            PopupUtil.cancelNotification(getApplicationContext());
        }
    }

    private void removeActiveMessage() {
        if (mPopupSwipeView.removeActiveMessage()) {
            myFinish();
        }
    }

    private void invalidView(Bundle b) {
        // Store bundle
        mBundle = b;

        // Create message from bundle
        MessageObject message = new MessageObject(getApplicationContext(), mBundle);
        mPopupSwipeView.addMessage(obc, message);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        // Re-invalid views with new sms intent data
        invalidView(intent.getExtras());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mBundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "---onResume---");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "---onPause---");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "---onStop---");
        ClearAllReceiver.removeCancel(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "---onDestroy---");
    }

    // for next is reply or delete or something else...
    /**
     * Reply to the current message, start the reply intent
     */
    private void goToMessageIndex(final boolean replyToThread) {
        // get current messageobject first
    	Intent reply = mMsgObj.getReplyIntent(replyToThread);
        getApplicationContext().startActivity(reply);
        finish();
    }

    private void deleteMessage() {
        MessageObject mo = mPopupSwipeView.getActiveMessage();
        if (mo != null) {
            mo.delete();
        } else {
            Toast.makeText(getApplicationContext(), res.getString(R.string.popup_msg_delete_error), 4000).show();
        }
    }

    private void quickReplyMessage(String send) {
        hideSoftKeyboard();
        if (send != null && send.length() > 0) {
            MessageObject mo = mPopupSwipeView.getActiveMessage();
            if (mo != null) {
                mo.replyToMessage(send);
                Toast.makeText(getApplicationContext(), res.getString(R.string.popup_msg_sending), Toast.LENGTH_LONG)
                        .show();
            } else {
                Log.d(TAG, "msg obj is null");
            }
        } else {
            Log.d(TAG, "reply msg is null");
        }
    }

    private void hideSoftKeyboard() {
        if (inputManager == null) {
            inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        inputManager.hideSoftInputFromWindow(mEditSend.getApplicationWindowToken(), 0);
    }
    
    private void showDelAlert() {
        final AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.show();
        Window window = dlg.getWindow();
        window.setContentView(R.layout.smspopup_del_dialog);
        Button ok = (Button) window.findViewById(R.id.popup_alert_del_btn);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteMessage();
                removeActiveMessage();
                Toast.makeText(getApplicationContext(), res.getString(R.string.popup_msg_delete), 4000).show();
                dlg.cancel();
            }
        });
     
        Button cancel = (Button) window.findViewById(R.id.popup_alert_clo_btn);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dlg.cancel();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { 
            MessageObject mo = null;
            List<MessageObject> msgs = mPopupSwipeView.getMessages();
            int size = msgs.size();
            for (int i = 0; i < size; i++) {
                mo = mPopupSwipeView.getActiveMessage();
                mo.setMessageRead();
                mPopupSwipeView.removeActiveMessage();
            }
            PopupUtil.cancelNotification(getApplicationContext());
            finish();
            return false;
        } 
        return false; 
    }
    
}
