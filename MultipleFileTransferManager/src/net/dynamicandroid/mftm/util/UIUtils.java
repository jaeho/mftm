package net.dynamicandroid.mftm.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.WindowManager;

public class UIUtils {

	public static boolean isGoogleTV(Context context) {
		return context.getPackageManager().hasSystemFeature("com.google.android.tv");
	}

	public static boolean hasFroyo() {
		// Can use static final constants like FROYO, declared in later versions
		// of the OS since they are inlined at compile time. This is guaranteed behavior.
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean hasGingerbread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	public static boolean hasICS() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	public static boolean isHoneycombTablet(Context context) {
		return hasHoneycomb() && isTablet(context);
	}

	public static boolean areU2x() {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1;
	}
	
	
	public static int getOrientation(Context context) {
		return context.getResources().getConfiguration().orientation;
	}

	public static int getScreenWidth(Context context) {
		WindowManager display = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		return display.getDefaultDisplay().getWidth();
	}

	public static int getScreenHeight(Context context) {
		WindowManager display = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		return display.getDefaultDisplay().getHeight();
	}
}
