package net.dynamicandroid.mftm;

public class MFTM {

	static MFTM mftm = null;
	
	private TransferService transferService = null;
	
	private String appName = null;
	
	public static MFTM getInstance() {
		if(mftm == null)
			mftm = new MFTM();
		
		return mftm;
	}
	
	public static MFTM initialize(String appName) {
		if(mftm == null)
			mftm = new MFTM();
			
		mftm.appName = appName;
		
		return mftm;
	}

	public TransferService getTransferService() {
		return transferService;
	}

	public void setTransferService(TransferService transferService) {
		this.transferService = transferService;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
	
}
