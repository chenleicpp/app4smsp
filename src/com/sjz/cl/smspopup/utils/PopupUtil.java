package com.sjz.cl.smspopup.utils;

import java.text.SimpleDateFormat;
import android.net.Uri;
import android.provider.Settings;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import java.util.Date;
import android.app.NotificationManager;

/**
 * For this is a util class
 * 
 * @author chenlei
 * 
 */
public class PopupUtil {

	private static final String TAG = "popup.PopupUtil";
	
	private static final int NOTIFICATION_ID = 123;

	public static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");

	public static final Uri THREAD_ID_CONTENT_URI = Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "threadID");

	public static final String SMSTO_URI = "smsto:";

	public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";

	public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
	public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");

	public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
	public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");

	public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "conversations");

	public static final int SDK_VERSION_ECLAIR = 5;
	public static boolean PRE_ECLAIR = PopupUtil.getSDKVersionNumber() < PopupUtil.SDK_VERSION_ECLAIR ? true : false;

	private static final String TIME_FORMAT_12_HOUR = "h:mm a";
	private static final String TIME_FORMAT_24_HOUR = "H:mm";

	public static class ContactIdentification {
		String contactId = null;
		String contactLookup = null;
		String contactName = null;

		public ContactIdentification(String _contactId, String _contactLookup, String _contactName) {
			contactId = _contactId;
			contactLookup = _contactLookup;
			contactName = _contactName;
		}

		public ContactIdentification(String _contactId, String _contactName) {
			contactId = _contactId;
			contactName = _contactName;
		}

		public String getContactId() {
			return contactId;
		}

		public String getContactLookup() {
			return contactLookup;
		}

		public String getContactName() {
			return contactName;
		}
	}

	public static String formatTimestamp(Context context, long timestamp) {
		String HOURS_24 = "24";
		String hours;
		hours = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);

		SimpleDateFormat mSDF = new SimpleDateFormat();
		if (HOURS_24.equals(hours)) {
			mSDF.applyLocalizedPattern(TIME_FORMAT_24_HOUR);
		} else {
			mSDF.applyLocalizedPattern(TIME_FORMAT_12_HOUR);
		}
		return mSDF.format(new Date(timestamp));
	}

	synchronized public static long findThreadIdFromAddress(Context context, String address) {
		if (address == null)
			return 0;

		String THREAD_RECIPIENT_QUERY = "recipient";

		Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
		uriBuilder.appendQueryParameter(THREAD_RECIPIENT_QUERY, address);

		long threadId = 0;

		Cursor cursor = context.getContentResolver().query(uriBuilder.build(),
				new String[] { ContactWrapper.getColumn(ContactWrapper.COL_CONTACT_ID) }, null, null, null);

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					threadId = cursor.getLong(0);
				}
			} finally {
				cursor.close();
			}
		}
		return threadId;
	}

	public static Intent getSmsToIntent(Context context, long threadId) {
		Intent popup = new Intent(Intent.ACTION_VIEW);

		int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

		popup.setFlags(flags);

		if (threadId > 0) {
			popup.setData(Uri.withAppendedPath(THREAD_ID_CONTENT_URI, String.valueOf(threadId)));
		} else {
			return getSmsInboxIntent();
		}
		return popup;
	}

	public static Intent getSmsToIntent(Context context, String phoneNumber) {

		Intent popup = new Intent(Intent.ACTION_SENDTO);

		int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

		popup.setFlags(flags);

		if (!"".equals(phoneNumber)) {
			popup.setData(Uri.parse(SMSTO_URI + Uri.encode(phoneNumber)));
		} else {
			return getSmsInboxIntent();
		}
		return popup;
	}

	public static Intent getSmsInboxIntent() {
		Intent conversations = new Intent(Intent.ACTION_MAIN);
		conversations.setType(SMS_MIME_TYPE);
		int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;
		conversations.setFlags(flags);
		return conversations;
	}

	public static int getSDKVersionNumber() {
		int version_sdk;
		try {
			version_sdk = Integer.valueOf(android.os.Build.VERSION.SDK);
		} catch (NumberFormatException e) {
			version_sdk = 0;
		}
		return version_sdk;
	}

	synchronized public static long findMessageId(Context context, long threadId, long timestamp, String body,
			int messageType) {

		long id = 0;
		String selection = "date like " + "'%" + timestamp / 1000 + "%'";
		final String sortOrder = "date DESC";
		final String[] projection = new String[] { "_id", "date", "thread_id", "body" };

		if (threadId > 0) {
			Log.d(TAG, "Trying to find message ID");
			if (MessageObject.MSG_TYPE_MMS == messageType) {
				selection += " and date = " + (timestamp / 1000);
			}

			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId), projection, selection, null,
					sortOrder);

			if (cursor != null) {
				try {
					if (cursor.moveToFirst()) {
						id = cursor.getLong(0);
						Log.d(TAG, "Message id found = " + id);
					}
				} finally {
					cursor.close();
				}
			}
		}
		return id;
	}

	public static void deleteMessage(Context context, long messageId, long threadId, int messageType) {

		if (messageId > 0) {
			Log.d(TAG, "id of message to delete is " + messageId);

			setMessageRead(context, messageId, messageType);

			Uri deleteUri;

			if (MessageObject.MSG_TYPE_MMS == messageType) {
				deleteUri = Uri.withAppendedPath(MMS_CONTENT_URI, String.valueOf(messageId));
			} else if (MessageObject.MSG_TYPE_SMS == messageType) {
				deleteUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
			} else {
				return;
			}

			int count = 0;
			try {
				count = context.getContentResolver().delete(deleteUri, null, null);
			} catch (Exception e) {
				Log.d(TAG, "deleteMessage(): Problem deleting message - " + e.toString());
			}

			Log.d(TAG, "Messages deleted: " + count);
		}
	}

	synchronized public static void setMessageRead(Context context, long messageId, int messageType) {

		if (messageId > 0) {
			ContentValues values = new ContentValues(2);
			values.put("read", 1);
			values.put("seen", 1);

			Uri messageUri;

			if (MessageObject.MSG_TYPE_MMS == messageType) {
				messageUri = Uri.withAppendedPath(MMS_INBOX_CONTENT_URI, String.valueOf(messageId));
			} else if (MessageObject.MSG_TYPE_SMS == messageType) {
				messageUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
			} else {
				return;
			}

			ContentResolver cr = context.getContentResolver();
			int result;
			try {
				result = cr.update(messageUri, values, null, null);
			} catch (Exception e) {
				result = 0;
			}
			Log.d(TAG, String.format("message id = %s marked as read, result = %s", messageId, result));
		}
	}

	synchronized public static ContactIdentification getPersonIdFromPhoneNumber(Context context, String address) {
		if (address == null)
			return null;

		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(ContactWrapper.getPhoneLookupContentFilterUri(), Uri.encode(address)),
				ContactWrapper.getPhoneLookupProjection(), null, null, null);

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					String contactId = String.valueOf(cursor.getLong(0));
					String contactName = cursor.getString(1);

					String contactLookup = null;

					if (!PRE_ECLAIR) {
						contactLookup = cursor.getString(2);
					}

					Log.d(TAG, "Found person: " + contactId + ", " + contactName + ", " + contactLookup);
					return new ContactIdentification(contactId, contactLookup, contactName);
				}
			} finally {
				cursor.close();
			}
		}

		return null;
	}

    public static void cancelNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

}
