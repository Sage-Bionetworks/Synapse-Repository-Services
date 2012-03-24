package org.sagebionetworks;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Note: This class in not GWT compatible and it outside of the GWT module which starts at org.sagebionetworks.model.
 * 
 * @author John
 *
 */
public class ResourceUtils {

	/**
	 * Read an input stream into a string.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
}
