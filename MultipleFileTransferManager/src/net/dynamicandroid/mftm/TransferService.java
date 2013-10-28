package net.dynamicandroid.mftm;

import net.dynamicandroid.mftm.util.Logger;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class TransferService extends Service {

	TransferServiceBinder mBinder = new TransferServiceBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d("TransferService Created");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.d("TransferService Destroyed");
		DownloadManager.destroy();
		UploadManager.destroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	public void executeDownload(AsyncTask<Void, Void, Boolean> task) {
		task.executeOnThreadPoolExecutor();
	}

	public void executeUpload(AsyncTask<Void, Void, String> task) {
		task.executeOnThreadPoolExecutor();
	}


	public class TransferServiceBinder extends Binder {
		public TransferService getService() {
			return TransferService.this;
		}
	}
}
