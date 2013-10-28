package net.dynamicandroid.mftm;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;

public class TransferRequestBuilder {
	
	private Context mContext = null;
	
	private String mUrl = null;
	private File mDownloadFile = null;
	
	private TransferListener mListener = null;
	
	private UnzipRequest mUnZipRequest = null;
	
	public TransferRequestBuilder(Context context) {
		super();
		this.mContext = context;
	}

	public TransferRequestBuilder setUrl(String url) {
		this.mUrl = url;
		return this;
	}
	
	public TransferRequestBuilder setDownloadFile(File file) {
		this.mDownloadFile = file;
		return this;
	}
	
	public TransferRequestBuilder setListener(TransferListener listener) {
		this.mListener = listener;
		return this;
	}
	
	public TransferRequestBuilder setUnZipRequest(UnzipRequest unZipRequest) {
		this.mUnZipRequest = unZipRequest;
		return this;
	}
	
	public TransferRequest download() {
		if(mContext == null || TextUtils.isEmpty(mUrl) || mDownloadFile==null)
			return null;
		
		TransferRequest request = new TransferRequest(mContext);
		request.setUrl(mUrl);
		request.setFile(mDownloadFile);
		
		if(mListener!=null)
			request.setListener(mListener);
		
		if(mUnZipRequest!=null)
			request.setUnZipRequest(mUnZipRequest);
		
		DownloadManager.getInstance().requestDownload(request);
		
		return request;
	}
	
}
