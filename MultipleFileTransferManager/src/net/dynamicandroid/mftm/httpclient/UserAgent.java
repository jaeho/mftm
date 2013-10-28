package net.dynamicandroid.mftm.httpclient;

import net.dynamicandroid.mftm.MFTM;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class UserAgent {
	private static String agent = null;

	public static String get(Context context) {
		
		
		if (agent == null) {
			String appInfo = "";
			try {
				String packageName = context.getPackageName();
		
				PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
				appInfo = packageName + "/" + info.versionCode;
			} catch (NameNotFoundException e) {
				
			}
			agent = MFTM.getInstance().getAppName()+" (" + Build.MODEL + "; Android " + Build.VERSION.RELEASE + ";" + context.getResources().getConfiguration().locale
					+ ";" + appInfo + ")";
		}
		return agent;
	}
}
