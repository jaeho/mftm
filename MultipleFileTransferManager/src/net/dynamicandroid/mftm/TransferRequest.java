package net.dynamicandroid.mftm;

import java.io.File;
import java.util.HashMap;

import android.app.Application;
import android.content.Context;

public class TransferRequest {

	private Context context;
	private long id = -1;
	private String keySuffix;
	
	private String url;
	private TransferListener listener;
	private File file;
	
	private UnzipRequest unZip = null;
	
	private HashMap<String, Object> params;
	private HashMap<String, String> responseHeader = new HashMap<String, String>();
	
	private String result = null;

	public TransferRequest(Context context) {
		super();
		setContext(context);

	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context.getApplicationContext();
	}

	public Application getApplication() {
		if (context != null)
			return (Application) context.getApplicationContext();
		else
			return null;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getKey() {
		return getUrl() + " : " + (keySuffix == null ? "" : keySuffix);
	}
	
	public void setKeySuffix(Object... params) {
		if (params == null || params.length == 0) {
			return;
		}

		StringBuilder builder = new StringBuilder();
		for (Object obj : params) {
			builder.append(obj);
			builder.append(":");
		}
		this.keySuffix = builder.toString();
	}

	public TransferListener getListener() {
		return listener;
	}

	public void setListener(TransferListener listener) {
		this.listener = listener;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public UnzipRequest getUnZipRequest() {
		return unZip;
	}

	public void setUnZipRequest(UnzipRequest unZip) {
		this.unZip = unZip;
	}

	public HashMap<String, Object> getParams() {
		return params;
	}

	public HashMap<String, Object> addParams(String key, Object value) {
		if (params == null)
			params = new HashMap<String, Object>();

		if (!params.containsKey(key)) {
			params.put(key, value);
			return params;
		}

		return null;
	}
	
	public void addHeader(String key, String value) {
		responseHeader.put(key, value);
	}
	
	public String getHeader(String key) {
		return responseHeader.get(key);
	}

	public boolean isEmpty() {
		return getUrl() == null || getFile() == null;
	}

	@Override
	public String toString() {
		return "FileDownloadRequest [id=" + id + ", url=" + url + ", listener=" + listener + ", file=" + file + "]";
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	
}
