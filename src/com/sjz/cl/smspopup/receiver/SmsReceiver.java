package com.sjz.cl.smspopup.receiver;


import com.sjz.cl.smspopup.service.BaseService;
import com.sjz.cl.smspopup.service.SmsReceiverService;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
/**
 * For this class is the main class that recieve the sms msg.
 * When the msg is arrived,it first go into here
 * @author chenlei
 *
 */

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SMSPopup:SmsReceiver";
    
    private static final String PRE_ENABLE_POPUP_KEY = "pref_enable_popup_key";
    
    private static final String CLASS_MMS_CONVERSATIONLIST = "com.android.mms.ui.ConversationList";
    private static final String CLASS_MMS_COMPOSEMESSAGEACTIVITY = "com.android.mms.ui.ComposeMessageActivity";
    private Context mCtx;
    private SharedPreferences mSPreferences;
    

    @Override
    public void onReceive(Context context, Intent intent) {
        //some init
        mCtx = context.getApplicationContext();
        mSPreferences = mCtx.getSharedPreferences(getDefaultSharedPreferencesName(mCtx),
                getDefaultSharedPreferencesMode());
        //read the enable key to decide show popup
        Boolean bEnable = mSPreferences.getBoolean("enable_service", true);
        /*
         * if top activity is at mms,not show popup windows
         */
        ActivityManager am = (ActivityManager) mCtx.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        if (bEnable && !TextUtils.equals(CLASS_MMS_CONVERSATIONLIST, cn.getClassName()) 
                && !TextUtils.equals(CLASS_MMS_COMPOSEMESSAGEACTIVITY, cn.getClassName())) {
            Log.d(TAG, "---receive mms from here---");
            intent.setClass(mCtx, SmsReceiverService.class);
            intent.putExtra("result", getResultCode());
            BaseService.startBaseService(mCtx, intent);
        }
    }
    
    private static String getDefaultSharedPreferencesName(Context context) {
        return "popup";
    }

    private static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

}
