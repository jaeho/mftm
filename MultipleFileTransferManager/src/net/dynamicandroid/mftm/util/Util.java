package net.dynamicandroid.mftm.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class Util {

	public static String streamToString(InputStream inputStream) throws IOException {
		return IOUtils.toString(inputStream);
	}
	
	
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
		}
	}
	
}
