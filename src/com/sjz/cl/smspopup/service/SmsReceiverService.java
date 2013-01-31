package com.sjz.cl.smspopup.service;

import java.util.List;

import com.sjz.cl.smspopup.R;
import com.sjz.cl.smspopup.utils.ManageWakeLock;
import com.sjz.cl.smspopup.utils.MessageObject;
import com.sjz.cl.smspopup.utils.SmsMessageSender;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.telephony.SmsMessage.MessageClass;
import android.util.Log;
import android.widget.Toast;

/**
 * For this service will deal with sms receive
 * 
 * @author chenlei
 * 
 */

public class SmsReceiverService extends BaseService {

    private static final String TAG = "SmsReceiverService";

    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    //private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    //private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

    public static final String ACTION_SMS_SENT = "com.android.mms.transaction.MESSAGE_SENT";

    private boolean serviceRestarted = false;

    private static final int TOAST_HANDLER_MESSAGE_SENT = 0;
    private static final int TOAST_HANDLER_MESSAGE_FAILED = 1;

    private Context mCtx;

    private int mResultCode;

    public SmsReceiverService() {
        super(TAG);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCtx = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceRestarted = false;
        if ((flags & START_FLAG_REDELIVERY) != 0) {
            serviceRestarted = true;
        }
        Log.d(TAG, "smsreceiverservice onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void handleReceived(Intent intent) {
        mResultCode = 0;
        if (intent != null && !serviceRestarted) {
            mResultCode = intent.getIntExtra("result", 0);
            final String action = intent.getAction();

            if (ACTION_SMS_RECEIVED.equals(action)) {
                // custom sms into here
                dispatchSMSReceive(intent);
            } else if (ACTION_SMS_SENT.equals(action)) {
                dispatchSMSSent(intent);
            }
        }
    }

    /**
     * 
     * @param intent
     */
    private void dispatchSMSReceive(Intent intent) {
        Log.d(TAG, "we need to look up here");
        // get msg content
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
            if (messages == null) {
                Log.d(TAG, "no message!");
                return;
            }
            if (messages.length == 0) {
                Log.d(TAG, "message length is zero!");
                return;
            }
            byte pduObjs[][] = new byte[messages.length][];
            for (int i = 0; i < messages.length; i++) {
                pduObjs[i] = (byte[]) messages[i];
            }
            byte[][] pdus = new byte[pduObjs.length][];
            int pduCount = pdus.length;
            SmsMessage[] msgs = new SmsMessage[pduCount];
            for (int i = 0; i < pduCount; i++) {
                pdus[i] = pduObjs[i];
                msgs[i] = SmsMessage.createFromPdu(pdus[i]);
            }
            // notifySmsReceive func
            if (msgs != null) {
                notifySmsReceive(new MessageObject(mCtx, msgs, System.currentTimeMillis()));
            }
        }
    }

    private void dispatchSMSSent(Intent intent) {
        Log.d(TAG, "SMSReceiver: Handle SMS sent");

        PackageManager pm = getPackageManager();
        Intent sysIntent = null;
        Intent tempIntent;
        List<ResolveInfo> receiverList;
        boolean forwardToSystemApp = true;

        // Search for system messaging app that will receive our
        // "message sent complete" type intent
        tempIntent = intent.setClassName(SmsMessageSender.MESSAGING_PACKAGE_NAME,
                SmsMessageSender.MESSAGING_RECEIVER_CLASS_NAME);

        receiverList = pm.queryBroadcastReceivers(tempIntent, 0);

        if (receiverList.size() > 0) {
            Log.d(TAG, "SMSReceiver: Found system messaging app - " + receiverList.get(0).toString());

            sysIntent = tempIntent;

            // This is quite a hack - it seems most OEMs will replace the stock
            // android messaging
            // app, however Samsung changes it but keeps the same package name.
            // One change is that
            // it won't finish moving the messaging from "outbox" to "sent" or
            // "failed" for us so
            // this checks to see if the modified samsung app is there and if
            // so, we'll handle the
            // final move of the message ourselves.

            // Only the samsung sms/mms apk has this modified compose class
            final Intent samsungIntent = new Intent();
            samsungIntent.setClassName(SmsMessageSender.MESSAGING_PACKAGE_NAME,
                    SmsMessageSender.SAMSUNG_MESSAGING_COMPOSE_CLASS_NAME);
            receiverList = pm.queryIntentActivities(samsungIntent, 0);
            if (receiverList.size() > 0) {
                // no stock system app found to finish the message move
                sysIntent = null;
            }
        }

        /*
         * No system messaging app was found to forward this intent to,
         * therefore we will need to do the final piece of this ourselves which
         * is basically moving the message to the correct folder depending on
         * the result.
         */
        if (sysIntent == null) {
            forwardToSystemApp = false;
            Log.d(TAG, "SMSReceiver: Did not find system messaging app, moving messages directly");

            Uri uri = intent.getData();

            if (mResultCode == Activity.RESULT_OK) {
                SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_SENT);
            } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF)
                    || (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
                SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_QUEUED);
            } else {
                SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_FAILED);
            }
        }

        // Check the result and notify the user using a toast
        if (mResultCode == Activity.RESULT_OK) {
            Log.d(TAG, "SMSReceiver: Message was sent");
            mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_SENT);

        } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF)
                || (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
            Log.d(TAG, "SMSReceiver: Error sending message (will send later)");
        } else {
            Log.d(TAG, "SMSReceiver: Error sending message");
            mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_FAILED);
        }

        /*
         * Start the broadcast via PendingIntent so result code is passed over
         * correctly
         */
        if (forwardToSystemApp) {
            try {
                Log.d(TAG, "SMSReceiver: Broadcasting send complete to system messaging app");
                PendingIntent.getBroadcast(this, 0, sysIntent, 0).send(mResultCode);
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg != null) {
                switch (msg.what) {
                case TOAST_HANDLER_MESSAGE_SENT:
                    Toast.makeText(SmsReceiverService.this,
                            getResources().getString(R.string.popup_msg_send_suc), Toast.LENGTH_SHORT)
                            .show();
                    break;
                case TOAST_HANDLER_MESSAGE_FAILED:
                    Toast.makeText(SmsReceiverService.this,
                            getResources().getString(R.string.popup_msg_send_fail), Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    };

    private void notifySmsReceive(MessageObject msg) {
        if (msg.isSms() && msg.getMessageClass() == MessageClass.CLASS_0) {
            return;
        }
        if ("".equals(msg.getMessageBody())) {
            return;
        }
        // now can let the screen light and popup window
        // when telephone call in that we dont show popup window
        TelephonyManager mTM = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;

        if (callStateIdle) {
            // let the screen light
        	ManageWakeLock.acquireFull(mCtx);
			startActivity(msg.getPopupIntent());
        } else {
            // do noting
            Log.d(TAG, "dont show popup sms");
        }
    }
}
