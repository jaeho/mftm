package net.dynamicandroid.mftm.util;

import android.util.Log;

public class Logger {

	final static String TAG = "mftm";

	public static void d(Object o) {

		Log.d(TAG, o.toString());

	}

	public static void e(Exception e) {

		Log.e(TAG, e.getLocalizedMessage());

	}

	public static void e(String e) {

		Log.e(TAG, e);

	}
}
