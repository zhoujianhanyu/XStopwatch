/*
 * XStopwatch / XTimer
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/xstopwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/xstopwatch/licensing.html
 */
package org.dwallach.xstopwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferencesHelper {
    private final static String TAG = "PreferencesHelper";

    public static void savePreferences(Context context) {
        Log.v(TAG, "savePreferences");
        SharedPreferences prefs = context.getSharedPreferences(Constants.sharedPrefsStopwatch, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StopwatchState stopwatchState = StopwatchState.getSingleton();

        editor.putLong(Constants.prefStopwatchStartTime, stopwatchState.getStartTime());
        editor.putLong(Constants.prefStopwatchBaseTime, stopwatchState.getPriorTime());
        editor.putBoolean(Constants.prefStopwatchRunning, stopwatchState.isRunning());
        editor.putBoolean(Constants.prefStopwatchReset, stopwatchState.isReset());
        editor.putLong(Constants.prefStopwatchUpdateTimestamp, stopwatchState.getUpdateTimestamp());

        if (!editor.commit())
            Log.v(TAG, "savePreferences commit failed ?!");

        prefs = context.getSharedPreferences(Constants.sharedPrefsTimer, Context.MODE_PRIVATE);
        editor = prefs.edit();

        TimerState timerState = TimerState.getSingleton();

        editor.putLong(Constants.prefTimerStartTime, timerState.getStartTime());
        editor.putLong(Constants.prefTimerPauseElapsed, timerState.getElapsedTime());
        editor.putLong(Constants.prefTimerDuration, timerState.getDuration());
        editor.putBoolean(Constants.prefTimerRunning, timerState.isRunning());
        editor.putBoolean(Constants.prefTimerReset, timerState.isReset());
        editor.putLong(Constants.prefTimerUpdateTimestamp, timerState.getUpdateTimestamp());

        if (!editor.commit())
            Log.v(TAG, "savePreferences commit failed ?!");
    }

    public static void broadcastPreferences(Context context, String action) {
        Log.v(TAG, "broadcastPreferences");
        StopwatchState stopwatchState = StopwatchState.getSingleton();
        TimerState timerState = TimerState.getSingleton();

        // There's a chance that our app is not running but we received the broadcast intent
        // asking for our state to be sent out again. In that case, we'll need to load up
        // the preferences again. Otherwise we'll be sending out incorrect data.

        if(!stopwatchState.isInitialized() || !timerState.isInitialized())
            loadPreferences(context);

        if(action == null || action.equals(Constants.stopwatchQueryIntent) || action.equals(Constants.stopwatchUpdateIntent)) {
            if (!stopwatchState.isInitialized()) {
                Log.e(TAG, "stopwatch state not initialized, can't broadcast preferences");
            } else {
                Log.v(TAG, "broadcasting stopwatch preferences");
                Intent broadcast = new Intent(Constants.stopwatchUpdateIntent);
                broadcast.putExtra(Constants.prefStopwatchStartTime, stopwatchState.getStartTime());
                broadcast.putExtra(Constants.prefStopwatchBaseTime, stopwatchState.getPriorTime());
                broadcast.putExtra(Constants.prefStopwatchRunning, stopwatchState.isRunning());
                broadcast.putExtra(Constants.prefStopwatchReset, stopwatchState.isReset());
                broadcast.putExtra(Constants.prefStopwatchUpdateTimestamp, stopwatchState.getUpdateTimestamp());
                context.sendBroadcast(broadcast);
            }
        }

        if(action == null || action.equals(Constants.timerQueryIntent) || action.equals(Constants.timerUpdateIntent)) {
            if (!timerState.isInitialized()) {
                Log.e(TAG, "timer state not initialized, can't broadcast preferences");
            } else {
                Log.v(TAG, "broadcasting timer preferences");
                Intent broadcast = new Intent(Constants.timerUpdateIntent);
                broadcast.putExtra(Constants.prefTimerStartTime, timerState.getStartTime());
                broadcast.putExtra(Constants.prefTimerPauseElapsed, timerState.getElapsedTime());
                broadcast.putExtra(Constants.prefTimerDuration, timerState.getDuration());
                broadcast.putExtra(Constants.prefTimerRunning, timerState.isRunning());
                broadcast.putExtra(Constants.prefTimerReset, timerState.isReset());
                broadcast.putExtra(Constants.prefTimerUpdateTimestamp, timerState.getUpdateTimestamp());
                context.sendBroadcast(broadcast);
            }
        }
    }

    public static void loadPreferences(Context context) {
        Log.v(TAG, "loadPreferences");

        StopwatchState stopwatchState = StopwatchState.getSingleton();
        // brackets just so that the variables go away when we leave scope
        {
            SharedPreferences prefs = context.getSharedPreferences(Constants.sharedPrefsStopwatch, Context.MODE_PRIVATE);

            long priorTime = prefs.getLong(Constants.prefStopwatchBaseTime, 0L);
            long startTime = prefs.getLong(Constants.prefStopwatchStartTime, 0L);
            boolean isRunning = prefs.getBoolean(Constants.prefStopwatchRunning, false);
            boolean isReset = prefs.getBoolean(Constants.prefStopwatchReset, true);
            long updateTimestamp = prefs.getLong(Constants.prefStopwatchUpdateTimestamp, 0L);

            Log.v(TAG, "Stopwatch:: startTime(" + startTime + "), priorTime(" + priorTime + "), isRunning(" + isRunning + "), isReset(" + isReset + "), updateTimestamp(" + updateTimestamp + ")" );

            stopwatchState.restoreState(priorTime, startTime, isRunning, isReset, updateTimestamp);
        }

        TimerState timerState = TimerState.getSingleton();
        {
            SharedPreferences prefs = context.getSharedPreferences(Constants.sharedPrefsTimer, Context.MODE_PRIVATE);

            long startTime = prefs.getLong(Constants.prefTimerStartTime, 0L);
            long pauseDelta = prefs.getLong(Constants.prefTimerPauseElapsed, 0L);
            long duration = prefs.getLong(Constants.prefTimerDuration, 0L);
            boolean isRunning = prefs.getBoolean(Constants.prefTimerRunning, false);
            boolean isReset = prefs.getBoolean(Constants.prefTimerReset, true);
            long updateTimestamp = prefs.getLong(Constants.prefTimerUpdateTimestamp, 0L);

            // sanity checking: if we're coming back from whatever, and we discover that we *used* to
            // be running, but we've gotten way past the deadline, then just reset things.
            long currentTime = System.currentTimeMillis();
            if(isRunning && startTime + duration < currentTime) {
                isReset = true;
                isRunning = false;
            }

            Log.v(TAG, "Timer:: startTime(" + startTime + "), pauseDelta(" + pauseDelta + "), duration(" + duration + "), isRunning(" + isRunning + "), isReset(" + isReset + "), updateTimestamp(" + updateTimestamp + ")");

            timerState.restoreState(context, duration, pauseDelta, startTime, isRunning, isReset, updateTimestamp);
        }
    }
}
