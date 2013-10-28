package net.dynamicandroid.mftm;

import java.io.File;

public class UnzipRequest {

	private File pathToExtracting;
	private UnzipListener listener;
	private boolean isSuccess = false;
	
	public static interface UnzipListener {
		public void onProgress(int progress, int total);
	}

	public UnzipListener getListener() {
		return listener;
	}

	public void setListener(UnzipListener listener) {
		this.listener = listener;
	}

	public File getPathToExtracting() {
		return pathToExtracting;
	}

	public void setPathToExtracting(File pathToExtracting) {
		this.pathToExtracting = pathToExtracting;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	
}
