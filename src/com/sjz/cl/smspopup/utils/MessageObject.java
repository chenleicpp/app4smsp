package com.sjz.cl.smspopup.utils;

import com.sjz.cl.smspopup.ui.PopupActivity;
import com.sjz.cl.smspopup.utils.PopupUtil.ContactIdentification;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.MessageClass;
import android.util.Log;

/**
 * For this class contain msg properties
 * It can be used to show UI on PopupActivity
 * @author chenlei
 *
 */
public class MessageObject {

    private static final String TAG = "popup.MessageObject";

    // bundle extra key
    public static final String EXTRA_FROM_ADDRESS = "extra_from_address";
    public static final String EXTRA_MESSAGE_BODY = "extra_message_body";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";
    public static final String EXTRA_THREADID = "extra_threadid";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_MESSAGEID = "extra_messageid";
    private static final String EXTRAS_CONTACT_ID = "extra_contact_id";
    private static final String EXTRAS_CONTACT_LOOKUP = "extra_contact_lookup";
    private static final String EXTRAS_CONTACT_NAME = "extra_contact_name";

    public static final int MSG_TYPE_SMS = 1;
    public static final int MSG_TYPE_MMS = 2;

    // save to Content reference
    private Context mContext;
    // save to sms from where
    private String mFromAddress;
    // save to sms body
    private String mMessageBody;
    // save to sms receive time
    private long mTimestamp = 0;
    // save type which sms or mms
    private int mType;
    // save sms thead id to mean which msg thread
    private long mThreadId;
    // save sms message id to mean which msg
    private long mMessageId;
    /* save contact info*/
    private String mContactId = null;
    private String mContactLookupKey = null;
    private String mContactName = null;

    private MessageClass mMessageClass;

    public MessageObject(Context context, SmsMessage messages[], long timestamp) {
        mContext = context;
        SmsMessage sms = messages[0];
        mTimestamp = timestamp;
        mFromAddress = sms.getOriginatingAddress();
        mType = MSG_TYPE_SMS;
        mMessageClass = sms.getMessageClass();
        String body = "";

        try {
            if (messages.length == 1 || sms.isReplace()) {
                body = sms.getDisplayMessageBody();
            } else {
                StringBuilder bodyText = new StringBuilder();
                for (int i = 0; i < messages.length; i++) {
                    bodyText.append(messages[i].getMessageBody());
                }
                body = bodyText.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMessageBody = body;
        
        ContactIdentification contactIdentify = null;
        contactIdentify = PopupUtil.getPersonIdFromPhoneNumber(context, mFromAddress);
        mContactName = PhoneNumberUtils.formatNumber(mFromAddress);
        if (contactIdentify != null) {
            mContactId = contactIdentify.contactId;
            mContactLookupKey = contactIdentify.contactLookup;
            mContactName = contactIdentify.contactName;
        }
    }

    public MessageObject(Context ctx, Bundle bundle) {
        mContext = ctx;
        mFromAddress = bundle.getString(EXTRA_FROM_ADDRESS);
        mMessageBody = bundle.getString(EXTRA_MESSAGE_BODY);
        mTimestamp = bundle.getLong(EXTRA_TIMESTAMP, 0);
        mType = bundle.getInt(EXTRA_TYPE, MSG_TYPE_MMS);
        mMessageId = bundle.getLong(EXTRA_MESSAGEID, 0);
        mContactId = bundle.getString(EXTRAS_CONTACT_ID);
        mContactLookupKey = bundle.getString(EXTRAS_CONTACT_LOOKUP);
        mContactName = bundle.getString(EXTRAS_CONTACT_NAME);
    }

    public String getMessageBody() {
        return mMessageBody;
    }

    public String getFromAddress() {
        return mFromAddress;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public boolean isSms() {
        return mType == MSG_TYPE_SMS;
    }

    public boolean isMms() {
        return mType == MSG_TYPE_MMS;
    }

    public MessageClass getMessageClass() {
        return mMessageClass;
    }

    public Intent getPopupIntent() {
        Intent intent = new Intent();
        intent.setClass(mContext, PopupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtras(makeBundle());
        return intent;
    }
    
    public String getContactId() {
        return mContactId;
    }

    public String getContactLookupKey() {
        return mContactLookupKey;
    }

    public Uri getContactLookupUri() {
        if (mContactId == null) {
            return null;
        }

        return Uri.withAppendedPath(Contacts.CONTENT_URI, mContactId);
    }

    public Bundle makeBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_FROM_ADDRESS, mFromAddress);
        bundle.putString(EXTRA_MESSAGE_BODY, mMessageBody);
        bundle.putLong(EXTRA_TIMESTAMP, mTimestamp);
        bundle.putLong(EXTRA_THREADID, mThreadId);
        bundle.putInt(EXTRA_TYPE, mType);
        bundle.putLong(EXTRA_MESSAGEID, mMessageId);
        bundle.putString(EXTRAS_CONTACT_ID, mContactId);
        bundle.putString(EXTRAS_CONTACT_LOOKUP, mContactLookupKey);
        bundle.putString(EXTRAS_CONTACT_NAME, mContactName);
        return bundle;
    }

    // when click the content text go to the msg intent
    public Intent getReplyIntent(boolean replyToThread) {
        if (mType == MSG_TYPE_MMS) {
            // need to be add
        } else if (mType == MSG_TYPE_SMS) {
            locateThreadId();
            /*
             * There are two ways to reply to a message, by "viewing" the
             * threadId or by sending a new message to the address. In most
             * cases we should just execute the former, but in some cases its
             * better to send a new message to an address (apps like Google
             * Voice will intercept this intent as they don't seem to look at
             * the threadId).
             */
            if (replyToThread && mThreadId > 0) {
                Log.d(TAG, "Replying by threadId: " + mThreadId);
                return PopupUtil.getSmsToIntent(mContext, mThreadId);
            } else {
                Log.d(TAG, "Replying by address: " + mFromAddress);
                return PopupUtil.getSmsToIntent(mContext, mFromAddress);
            }
        }
        return null;
    }

    public void delete() {
        PopupUtil.deleteMessage(mContext, getMessageId(), mThreadId, mType);
    }

    public void locateThreadId() {
        if (mThreadId == 0) {
            mThreadId = PopupUtil.findThreadIdFromAddress(mContext, mFromAddress);
        }
    }

    public long getThreadId() {
        locateThreadId();
        return mThreadId;
    }

    public void locateMessageId() {
        if (mMessageId == 0) {
            if (mThreadId == 0) {
                locateThreadId();
            }
            mMessageId = PopupUtil.findMessageId(mContext, mThreadId, mTimestamp, mMessageBody, mType);
        }
    }

    public long getMessageId() {
        locateMessageId();
        return mMessageId;
    }

    public boolean replyToMessage(String quickReply) {

        // Mark the message we're replying to as read
        setMessageRead();

        // Send new message
        SmsMessageSender sender = new SmsMessageSender(mContext, new String[] { mFromAddress }, quickReply, getThreadId());

        return sender.sendMessage();
    }
    
    public void setMessageRead() {
        locateMessageId();
        PopupUtil.setMessageRead(mContext, mMessageId, mType);
    }
}
