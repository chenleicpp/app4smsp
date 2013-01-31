package com.sjz.cl.smspopup.utils;

import com.sjz.cl.smspopup.receiver.ClearAllReceiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * For this class is used for when keyguard disable,let the screen on and
 * keyboard on It is the force function
 * 
 * @author chenlei
 * 
 */
public class ManageWakeLock {

	private static final String TAG = "SMSPopup:ManageWakeLock";

	private static volatile PowerManager.WakeLock mWakeLock = null;
	private static volatile PowerManager.WakeLock mPartialWakeLock = null;
	private static final boolean PREFS_SCREENON_DEFAULT = true;
	private static final int PREFS_TIMEOUT_DEFAULT = 10;

	public static synchronized void acquireFull(Context mContext) {
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
			return;
		}

		PowerManager mPm = (PowerManager) mContext
				.getSystemService(Context.POWER_SERVICE);
		SharedPreferences mPrefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		// Let the cpu running,and keep the screen on in high light way,allow to
		// close LED keylight
		int flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;

		if (mPrefs.getBoolean("pref_enable_screenon_key",
				PREFS_SCREENON_DEFAULT)) {
			// Force to light the screen and LED keylight
			flags |= PowerManager.ACQUIRE_CAUSES_WAKEUP
					| PowerManager.ON_AFTER_RELEASE;
		}

		mWakeLock = mPm.newWakeLock(flags, TAG + ".full");
		mWakeLock.setReferenceCounted(false);
		mWakeLock.acquire();
		int timeout = PREFS_TIMEOUT_DEFAULT;
		// timeout for screen off
		ClearAllReceiver.setCancel(mContext, timeout);
	}

	public static synchronized void acquirePartial(Context mContext) {
		if (mPartialWakeLock != null)
			return;

		PowerManager mPm = (PowerManager) mContext
				.getSystemService(Context.POWER_SERVICE);

		// keep cpu running,but screen or LED keylight maybe not on
		mPartialWakeLock = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG
				+ ".partial");
		mPartialWakeLock.setReferenceCounted(false);

		mPartialWakeLock.acquire();
	}

	public static synchronized void releaseFull() {
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	public static synchronized void releasePartial() {
		if (mPartialWakeLock != null && mPartialWakeLock.isHeld()) {
			mPartialWakeLock.release();
			mPartialWakeLock = null;
		}
	}

	public static synchronized void releaseAll() {
		releaseFull();
		releasePartial();
	}

}