package net.dynamicandroid.mftm;

public interface TransferListener {

	void onPreTransfer(TransferRequest request);

	void onProgress(int progress, int total);

	void onCompleted(TransferRequest request);

	void onFailed(TransferRequest request);

	void onCanceled(TransferRequest request);

	void onPostTransfer(TransferRequest request);

}
