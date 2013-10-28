package net.dynamicandroid.mftm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

//TODO 업로드 다운로드 동시 진행의 경우 염두에 두고 수정이 필요함.
public abstract class TransferManager {

	public TransferService transferService;

	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			transferService = ((TransferService.TransferServiceBinder) service).getService();
			MFTM.getInstance().setTransferService(transferService);
			TransferManager.this.onServiceConnected();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			transferService = null;
		}
	};
	
	public void startService(TransferRequest request) {
		if(MFTM.getInstance().getTransferService()!=null)
			return;
		Intent intent = new Intent(request.getContext(), TransferService.class);
		request.getContext().startService(intent);
		request.getContext().bindService(intent , mConnection, Context.BIND_AUTO_CREATE);
	}
	
	public void unbindService(Context context) {
		context.unbindService(mConnection);
		context.stopService(new Intent(context , TransferService.class));
		MFTM.getInstance().setTransferService(null);
	}
	
	public abstract void onServiceConnected();
	public abstract boolean isEmpty();
}
