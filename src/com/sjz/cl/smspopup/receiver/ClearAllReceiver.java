package com.sjz.cl.smspopup.receiver;

import com.sjz.cl.smspopup.utils.ManageWakeLock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class provides an easy way to clear held WakeLocks and re-enable the Keyguard.
 * @author Foreigner
 *
 */

public class ClearAllReceiver extends BroadcastReceiver {
    
    private static final String TAG = "popup.ClearAllReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"ClearAllReceiver: onReceive()");
        clearAll();
    }

    /*
     * Release WakeLocks and re-enable Keyguard (if reenableKeyguard is true). The only time we
     * don't want to re-enable the Keyguard is if the user is trying to exit the Keyguard securely
     * (exitKeyguardSecurely()) to perform an action like "Reply" or "Inbox".
     */
    public static synchronized void clearAll(boolean reenableKeyguard) {
//        Log.d(TAG,"ClearAllReceiver: clearAll(" + reenableKeyguard + ")");
//        if (reenableKeyguard) {
//            ManageKeyguard.reenableKeyguard();
//        }
    	Log.d(TAG, "ClearAllReceiver: ClearAll WakeLock");
        ManageWakeLock.releaseAll();
    }

    /*
     * Default to re-enabling Keyguard
     */
    public static synchronized void clearAll() {
        clearAll(true);
    }

    /*
     * Gets the PendingIntent for a Broadcast to this class
     */
    private static PendingIntent getPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context, 0, new Intent(context, ClearAllReceiver.class), 0);
    }

    /*
     * Schedules a Broadcast to this receiver for some time in the future (timeout). Used in the
     * case the user doesn't notice the popup and the app and phone should go back to sleep (turning
     * off screen and locking the keyguard again).
     */
    public static synchronized void setCancel(Context context, int timeout) {
        removeCancel(context);
        Log.d(TAG,"ClearAllReceiver: setCancel() for " + timeout + " seconds");
        AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        myAM.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (timeout * 1000),
                getPendingIntent(context));
    }

    /*
     * Cancels the scheduled Broadcast to this receiver (the user took some action so the Activity
     * can now react to whatever they are doing).
     */
    public static synchronized void removeCancel(Context context) {
        Log.d(TAG,"ClearAllReceiver: removeCancel()");
        AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        myAM.cancel(getPendingIntent(context));
    }
}
