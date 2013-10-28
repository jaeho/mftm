package net.dynamicandroid.mftm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import net.dynamicandroid.mftm.util.Logger;
import net.dynamicandroid.mftm.util.Util;

import org.apache.commons.io.FilenameUtils;

import android.os.Handler;
import android.webkit.MimeTypeMap;

public class UploadManager extends TransferManager {

	public static final int MAX_UPLOAD_COUNT = 3;
	private LinkedList<Long> requestIds = new LinkedList<Long>();
	private LinkedList<UploadTask> requestedTasks = new LinkedList<UploadTask>();
	private ConcurrentHashMap<Long, UploadTask> taskMap = new ConcurrentHashMap<Long, UploadTask>();
	private LinkedList<Long> taskInUploading = new LinkedList<Long>();
	private ConcurrentHashMap<Long, TransferRequest> requestMap = new ConcurrentHashMap<Long, TransferRequest>();
	private ConcurrentHashMap<String, Long> unfinishedRequests = new ConcurrentHashMap<String, Long>();

	private static UploadManager uploadManager;
	Handler handler = new Handler();

	public static UploadManager getInstance() {
		if (uploadManager == null) {
			uploadManager = new UploadManager();
		}

		return uploadManager;
	}

	public static void destroy() {
		if (uploadManager != null) {
			uploadManager = null;
			Logger.d("UploadManager Destroyed");
		}
	}

	public TransferRequest getTransferRequest(long id) {
		return requestMap.get(id);
	}

	public TransferRequest requestUpload(TransferRequest request) {
		if (request.getId() != -1 && requestIds.contains(request.getId()))
			return request;

		if (unfinishedRequests.containsKey(request.getKey())) {
			TransferRequest oldRequest = getTransferRequest(unfinishedRequests.get(request.getKey()));
			if (request.getListener() != null)
				oldRequest.setListener(request.getListener());
			request = null;
			return oldRequest;
		}

		transferService = MFTM.getInstance().getTransferService();

		long tempId = System.currentTimeMillis();
		if (requestIds.contains(tempId))
			request.setId(requestIds.getLast() + 1);
		else
			request.setId(tempId);

		addRequest(request);

		return request;
	}
	
	public void cancelUpload(TransferRequest request) {
		long requestId = request.getId();
		if (taskMap.containsKey(requestId)) {
			if (taskInUploading.contains(requestId))
				taskMap.get(requestId).cancel(false);

			removeTask(request);
			requestUpload();
		}
	}

	private void addRequest(TransferRequest request) {
		requestIds.add(request.getId());
		requestMap.put(request.getId(), request);
		unfinishedRequests.put(request.getKey(), request.getId());

		UploadTask task = new UploadTask(request);

		requestedTasks.add(task);
		taskMap.put(request.getId(), task);

		if (transferService == null) {
			startService(request);
			return;
		}

		requestUpload();
	}

	private void removeTask(TransferRequest request) {
		taskMap.remove(request.getId());
		taskInUploading.remove(request.getId());
		unfinishedRequests.remove(request.getKey());
		requestIds.remove(request.getId());
		requestMap.remove(request.getId());
	}

	private void requestUpload() {
		if (transferService != null && taskInUploading.size() <= MAX_UPLOAD_COUNT && requestedTasks.size() > 0) {
			transferService.executeUpload(requestedTasks.getFirst());
			requestedTasks.removeFirst();
		} else
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (isEmpty()) {
						destroy();
						if (DownloadManager.getInstance().isEmpty()) {
								if (transferService != null && MFTM.getInstance().getTransferService() != null) {
								// TODO 딜레이로 인해 java.lang.IllegalArgumentException: Service not registered 예외 처리 
								try {
									unbindService(transferService.getApplicationContext());
								} catch (Exception e) {
									Logger.e(e);
								}
							}
						}
					}
				}
			}, 1000);
	}

	class UploadTask extends AsyncTask<Void, Void, String> {

		final String CRLF = "\r\n";
		private ArrayList<Object> list = new ArrayList<Object>();
		private TransferRequest request;
		private File uploadFile = null;

		public UploadTask(TransferRequest request) {
			super();
			this.request = request;

			if (request.getParams() != null)
				for (String key : request.getParams().keySet()) {
					Object value = request.getParams().get(key);
					if (value instanceof String)
						addParameter(key, value.toString());
					else if (value instanceof File && uploadFile == null) { // 하나 이상의 파일은 무시
						uploadFile = (File) value;
						addFile(key, (File) value);
					}
				}
		}

		@Override
		protected String doInBackground(Void... arg0) {

			if (uploadFile == null) // 파일 첨부가 안되어있을 경우 꽝
				return null;

//			int transferred = 0;
			int total = (int) uploadFile.length();

			try {
				HttpURLConnection conn = (HttpURLConnection) new URL(request.getUrl()).openConnection();

				String delimiter = makeDelimiter();
				byte[] newLineBytes = CRLF.getBytes();
				byte[] delimeterBytes = delimiter.getBytes();
				byte[] dispositionBytes = "Content-Disposition: form-data; name=".getBytes();
				byte[] quotationBytes = "\"".getBytes();
				byte[] contentTypeBytes = ("Content-Type: " + getMimeType(uploadFile)).getBytes();
				byte[] fileNameBytes = "; filename=".getBytes();
				byte[] twoDashBytes = "--".getBytes();

				conn.addRequestProperty("Connection", "close");
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + delimiter);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
//				conn.setChunkedStreamingMode(1024 * 8);
				conn.setChunkedStreamingMode(1024);

				CountingOutputStream out = null;
//				OutputStream out = null;
				try {
					out = new CountingOutputStream(conn.getOutputStream(), total, request.getListener());
//					out = conn.getOutputStream();
					Object[] obj = new Object[list.size()];
					list.toArray(obj);

					for (int i = 0; i < obj.length; i += 2) {
						// delimiter 전송
						out.write(twoDashBytes);
						out.write(delimeterBytes);
						out.write(newLineBytes);
						// 파라미터 이름 출력
						out.write(dispositionBytes);
						out.write(quotationBytes);
						out.write(((String) obj[i]).getBytes());
						out.write(quotationBytes);
						if (obj[i + 1] instanceof String) {
							// String 이라면
							out.write(newLineBytes);
							out.write(newLineBytes);
							// 값 출력
							out.write(((String) obj[i + 1]).getBytes());
							out.write(newLineBytes);
						} else {
							if (obj[i + 1] instanceof File) {
								File file = (File) obj[i + 1];
								// File이 존재하는 지 검사한다.
								out.write(fileNameBytes);
								out.write(quotationBytes);
								String filename = file.getName();
								out.write(filename.getBytes());
								out.write(quotationBytes);
							}
							out.write(newLineBytes);
							out.write(contentTypeBytes);
							out.write(newLineBytes);
							out.write(newLineBytes);

							// File 데이터를 전송한다.
							if (obj[i + 1] instanceof File) {
								File file = (File) obj[i + 1];
								// file에 있는 내용을 전송한다.
								BufferedInputStream is = null;
								try {
									is = new BufferedInputStream(new FileInputStream(file), 1024);
									byte[] fileBuffer = new byte[1024 * 8]; // 8k
									int len = -1;
									while ((len = is.read(fileBuffer)) != -1) {
										out.write(fileBuffer, 0, len);
										
//										transferred += len;

										if (isCancelled()) {
											return null;
										}

									}
								} finally {
									if (is != null)
										try {
											is.close();
										} catch (IOException ex) {
											ex.printStackTrace();
										}
								}
							}
							out.write(newLineBytes);
						} // 파일 데이터의 전송 블럭 끝
						if (i + 2 == obj.length) {
							out.write(twoDashBytes);
							out.write(delimeterBytes);
							out.write(twoDashBytes);
							out.write(newLineBytes);
						}
					}

					out.flush();
				} finally {
					Util.closeQuietly(out);
				}

				return getResult(conn);
			} catch (MalformedURLException e) {
				Logger.e(e);
			} catch (ProtocolException e) {
				Logger.e(e);
			} catch (FileNotFoundException e) {
				Logger.e(e);
			} catch (IOException e) {
				Logger.e(e);
			}

			return null;
		}

		@Override
		protected void onCancelled() {

			super.onCancelled();

			if (request.getListener() != null)
				request.getListener().onCanceled(request);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			request.setResult(result);

			if (result != null) {
				if (request.getListener() != null)
					request.getListener().onCompleted(request);
			} else {
				if (request.getListener() != null)
					request.getListener().onFailed(request);
			}

			if (request.getListener() != null)
				request.getListener().onPostTransfer(request);

			removeTask(request);
			requestUpload();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (request.getListener() != null)
				request.getListener().onPreTransfer(request);

			taskInUploading.add(request.getId());
		}

		public TransferRequest getRequest() {
			return request;
		}

		private String getResult(HttpURLConnection conn) throws UnsupportedEncodingException, IOException {
			try {
				return Util.streamToString(conn.getInputStream());
			} catch (IOException e){
				Logger.e(Util.streamToString(conn.getErrorStream()));
				throw e;
			}
		}

		public void addParameter(String parameterName, String parameterValue) {
			if (parameterValue == null)
				throw new IllegalArgumentException("parameterValue can't be null!");

			list.add(parameterName);
			list.add(parameterValue);
		}

		public void addFile(String parameterName, File parameterValue) {
			if (parameterValue == null)
				throw new IllegalArgumentException("parameterValue can't be null!");

			list.add(parameterName);
			list.add(parameterValue);
		}

		private String makeDelimiter() {
			return "-----------------------n-c-party--";
		}
	}

	public static String getMimeType(File file) {
		String type = null;
		String extension = FilenameUtils.getExtension(file.getName());
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension);
		}
		return type;
	}

	public class CountingOutputStream extends BufferedOutputStream {
		private long transferred;
		private long total;
		private TransferListener listener;

		public CountingOutputStream(final OutputStream out, long total, TransferListener listener) {
			super(out);
			this.listener = listener;
			this.transferred = 0;
			this.total = total;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			out.flush();

			this.transferred += len;
			if (listener != null) {
				listener.onProgress((int) transferred, (int) total);
			}
		}

	}

	@Override
	public void onServiceConnected() {
		requestUpload();
	}

	@Override
	public boolean isEmpty() {
		return taskInUploading.size() == 0 && requestedTasks.size() == 0;
	}

}
