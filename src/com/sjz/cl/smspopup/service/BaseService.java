package com.sjz.cl.smspopup.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * This class is base service that other service extend it. Why I use service,I
 * am afraid that when os memery is low may kill the popup service so I want to
 * extende the lifetime to make it not killed by system easily. Use
 * IntentService to make sure in service run in a thread avoid ANR
 * 
 * @author chenlei
 * 
 */

abstract public class BaseService extends IntentService {

	private static final String TAG = "SMSPopup:BaseService";

	private static final String POWER_NAME = "com.sjz.cl.smspopup.service.BaseService";
	private static volatile PowerManager.WakeLock lockStatic = null;

	// This function is what subclass impliment,^_^
	abstract protected void handleReceived(Intent intent);

	public BaseService(String name) {
		super(name);
		setIntentRedelivery(true);
	}

	public static void startBaseService(Context ctxt, Intent i) {
		getLock(ctxt.getApplicationContext()).acquire();
		ctxt.startService(i);
	}

	// Ensures that the CPU is running
	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					POWER_NAME);
			lockStatic.setReferenceCounted(true);
		}

		return (lockStatic);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ((flags & START_FLAG_REDELIVERY) != 0) {
			// if service restart,then quick grab the lock
			getLock(this.getApplicationContext()).acquire();
		}
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			handleReceived(intent);
		} finally {
			getLock(this.getApplicationContext()).release();
		}
	}

}
