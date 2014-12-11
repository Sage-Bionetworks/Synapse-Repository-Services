package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOTestUtil {
	
	public static String readFromInputStream(InputStream is, String charSet) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (n>-1) {
				n = is.read(buffer);
				if (n>0) baos.write(buffer, 0, n);
			}
			return baos.toString(charSet);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
				baos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
