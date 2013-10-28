package net.dynamicandroid.mftm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import net.dynamicandroid.mftm.httpclient.HttpMethod;
import net.dynamicandroid.mftm.util.Logger;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;

public class DownloadManager extends TransferManager {

	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = CONNECT_TIMEOUT * 2;

	public static final int MAX_DOWNLOAD_COUNT = 7;

	private LinkedList<Long> requestIds = new LinkedList<Long>();
	private LinkedList<DownloadTask> requestedTasks = new LinkedList<DownloadManager.DownloadTask>();
	private ConcurrentHashMap<Long, DownloadTask> taskMap = new ConcurrentHashMap<Long, DownloadManager.DownloadTask>();
	private LinkedList<Long> taskInDownloading = new LinkedList<Long>();
	private ConcurrentHashMap<String, Long> unfinisheRequests = new ConcurrentHashMap<String, Long>();
	private ConcurrentHashMap<Long, TransferRequest> requestMap = new ConcurrentHashMap<Long, TransferRequest>();

	private static DownloadManager fileDownloadManager;
	Handler handler = new Handler();

	public static DownloadManager getInstance() {
		if (fileDownloadManager == null) {
			fileDownloadManager = new DownloadManager();
		}

		return fileDownloadManager;
	}

	public static void destroy() {
		if(fileDownloadManager!=null) {
			fileDownloadManager = null;
			Logger.d("DownloadManager Destroyed");
		}
	}

	public TransferRequest getTransferRequest(long id) {
		return requestMap.get(id);
	}

	public List<Long> getIdInDownloading() {
		return taskInDownloading;
	}

	/**
	 * 다운로드를 요청합니다.
	 * @param request
	 * @return
	 */
	public TransferRequest requestDownload(TransferRequest request) {
		if (request.getId() != -1 && requestIds.contains(request.getId()))
			return request;

		if(unfinisheRequests.containsKey(request.getKey())) {
			TransferRequest oldRequest = getTransferRequest(unfinisheRequests.get(request.getKey()));
			if(request.getListener()!=null) 
				oldRequest.setListener(request.getListener());
			if(request.getUnZipRequest()!=null) {
				oldRequest.setUnZipRequest(request.getUnZipRequest());
			}
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

	/**
	 * 요청했던 다운로드를 취소합니다.
	 * @param requestId
	 */
	public void cancelDownload(TransferRequest request) {
		long requestId = request.getId();
		if (taskMap.containsKey(requestId)) {
			if (taskInDownloading.contains(requestId))
				taskMap.get(requestId).cancel(false);

			removeTask(request);
			requestDownload();
		}
	}

	private void addRequest(TransferRequest request) {
		requestIds.add(request.getId());
		requestMap.put(request.getId(), request);
		unfinisheRequests.put(request.getKey(), request.getId());

		DownloadTask task = new DownloadTask(request);

		requestedTasks.add(task);
		taskMap.put(request.getId(), task);

		if(transferService==null) {
			startService(request);
			return;
		}

		requestDownload();
	}
	private void requestDownload() {
		if (transferService!=null && taskInDownloading.size() <= MAX_DOWNLOAD_COUNT && requestedTasks.size() > 0) {
			transferService.executeDownload(requestedTasks.getFirst());
			requestedTasks.removeFirst();
		} else
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(isEmpty()) {
						destroy();
						if(UploadManager.getInstance().isEmpty()) {
							if (transferService != null && MFTM.getInstance() != null) {
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

	public TransferRequest directDownload(TransferRequest request) throws MalformedURLException, IOException {
		InputStream inputStream = createInputStream(request);
		FileOutputStream fileOutput = new FileOutputStream(request.getFile());
		byte[] buffer = new byte[1024];
		int bufferLength = 0;
		while ((bufferLength = inputStream.read(buffer)) != -1) {
			fileOutput.write(buffer, 0, bufferLength);
		}
		fileOutput.close();
		return request;
	}

	InputStream createInputStream(TransferRequest request) throws MalformedURLException, IOException {
		HttpURLConnection urlConnection = createConnection(new URL(request.getUrl()), HttpMethod.GET);
		urlConnection.connect();
		return urlConnection.getInputStream();
	}

	class DownloadTask extends AsyncTask<Void, Void, Boolean> {

		private TransferRequest request;

		public DownloadTask(TransferRequest request) {
			super();
			this.request = request;
		}

		void getHeaderInfo(TransferRequest request, HttpURLConnection conn) {
			for (int i = 0;; i++) {
				String headerName = conn.getHeaderFieldKey(i);
				String headerValue = conn.getHeaderField(i);
				Logger.d("Read Header Information : " + headerName + " / " + headerValue);
				request.addHeader(headerName, headerValue);
				
				if (headerName == null && headerValue == null) {
					Logger.d("No more headers");
					break;
				}
			}
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			// TODO Auto-generated method stub

			if (request == null || request.isEmpty())
				return false;

			try {
				// DOWNLOAD
				URL url = new URL(request.getUrl());

				HttpURLConnection urlConnection = createConnection(url, HttpMethod.GET);
				urlConnection.connect();
				
				getHeaderInfo(request, urlConnection);
				
				FileOutputStream fileOutput = new FileOutputStream(request.getFile());
				InputStream inputStream = urlConnection.getInputStream();

				int totalSize = urlConnection.getContentLength();
				int downloadedSize = 0;

				byte[] buffer = new byte[1024];
				int bufferLength = 0;
				
				while ((bufferLength = inputStream.read(buffer)) != -1) {
					if (isCancelled()) {
						return false;
					}

					fileOutput.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;
					if (request.getListener() != null)
						request.getListener().onProgress(downloadedSize, totalSize);
				}
				fileOutput.close();

				//UNZIP
				try {
					UnzipRequest unzipRequest = request.getUnZipRequest();
					if(unzipRequest!=null) {
						File resultPath = unzipRequest.getPathToExtracting();
						if(resultPath==null) 
							resultPath = new File(getTempResultPath());
						else {
							if(!resultPath.exists())
								resultPath.mkdirs();
						}
						FileInputStream fileInputStream = new FileInputStream(request.getFile());
						ZipInputStream zis = null;
						ZipEntry zipEntry = null;
						ZipFile zipFile = null;
						
						try {
							zis = new ZipInputStream(new BufferedInputStream(fileInputStream));
							
							int unzipCount = 0;
							zipFile = new ZipFile(request.getFile());
							int zipCount = zipFile.size();
	
							while ((zipEntry = zis.getNextEntry()) != null) {
								if(isCancelled())
									return false;
								unzipCount++;
								unZip(zis, zipEntry, buffer, resultPath, unzipRequest, unzipCount, zipCount);
							}
						} catch (Exception e) {
							Logger.e(e);
							return false;
						} finally {
							if (zipFile != null) 
								zipFile.close();
							if (zis != null) 
								zis.close();
						}
						
						unzipRequest.setSuccess(true);
						unzipRequest.setPathToExtracting(resultPath);
						request.setUnZipRequest(unzipRequest);
					}
				} catch (Exception e) {
					// 언집은 다운로드의 결과에 영향을 안미치기 위해서 따로 익셉션을 캐치하는거랍니다. 보기 좋진 않군요
					Logger.e(e);
				}

				return true;
			} catch (MalformedURLException e) {
				Logger.e(e);
			} catch (IOException e) {
				Logger.e(e);
			}

			return false;
		}

		void unZip(ZipInputStream zis, ZipEntry zipEntry, byte[] buffer, File resultPath, UnzipRequest unzipRequest, int unzipCount, int zipCount) throws IOException {
			String filename = zipEntry.getName();
			File file = new File(resultPath.getAbsolutePath(), filename);
			if (zipEntry.isDirectory()) {
				file.mkdirs();
			} else {
				if (filename.contains("/")) {
					String folder = filename.substring(0, filename.lastIndexOf("/"));
					File folders = new File(resultPath.getAbsolutePath(), folder);
					folders.mkdirs();
				}
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
				buffer = new byte[1024];
				int readCount = 0;
				while ((readCount = zis.read(buffer)) != -1) {
					os.write(buffer, 0, readCount);
				}

				if (unzipRequest.getListener() != null)
					unzipRequest.getListener().onProgress( unzipCount , zipCount );

				os.close();
			}
		}

		String getTempResultPath() {
			String originalFilePath = request.getFile().getParentFile().getAbsolutePath();
			if(originalFilePath==null)
				originalFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
			return originalFilePath;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			if (request.getListener() != null)
				request.getListener().onPreTransfer(request);

			taskInDownloading.add(request.getId());
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (result) {
				if (request.getListener() != null)
					request.getListener().onCompleted(request);
			} else {
				request.getFile().delete();
				if (request.getListener() != null)
					request.getListener().onFailed(request);
			}

			if (request.getListener() != null)
				request.getListener().onPostTransfer(request);

			removeTask(request);
			requestDownload();
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();

			request.getFile().delete();
			if (request.getListener() != null)
				request.getListener().onCanceled(request);
		}

		public TransferRequest getRequest() {
			return request;
		}
	}

	private void removeTask(TransferRequest request) {
		taskMap.remove(request.getId());
		taskInDownloading.remove(request.getId());
		unfinisheRequests.remove(request.getKey());
		requestIds.remove(request.getId());
		requestMap.remove(request.getId());
	}

	public Bitmap getBitmapFromUrl(String url) {
		HttpURLConnection conn = null;
		InputStream is = null;
		Bitmap bitmap = null;

		try {
			conn = createConnection(new URL(url), HttpMethod.GET);
			is = (InputStream) conn.getInputStream();
			bitmap = BitmapFactory.decodeStream(is, new Rect(0, 0, 0, 0), defaultBitmapOption(1));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Logger.e(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.e(e);
		}

		return bitmap;
	}

	private Options defaultBitmapOption(int scale) {
		Options options = new Options();
		options.inSampleSize = scale;
		options.inPreferredConfig = Config.RGB_565;
		return options;
	}

	public static HttpURLConnection createConnection(URL url, HttpMethod method) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.addRequestProperty("Connection", "close");
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);

		conn.setDoInput(true);
		if (method == HttpMethod.GET) {
			conn.setDoOutput(false);
			conn.setRequestMethod(HttpMethod.GET.name());
		} else {
			conn.setDoOutput(true);
			conn.setRequestMethod(HttpMethod.POST.name());
			conn.setChunkedStreamingMode(0);
		}

		return conn;
	}

	@Override
	public void onServiceConnected() {
		// TODO Auto-generated method stub
		requestDownload();
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return taskInDownloading.size()==0 && requestedTasks.size()==0;
	}


}
