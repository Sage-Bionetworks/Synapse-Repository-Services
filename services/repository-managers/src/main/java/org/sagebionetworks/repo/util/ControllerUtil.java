package org.sagebionetworks.repo.util;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;

public class ControllerUtil {
	/**
	 * Reads in the request body and returns it as a string
	 * This is useful for capturing small requests which need to be used more than once
	 * @param request
	 * @return
	 * @throws DatastoreException
	 */
	public static String getRequestBodyAsString(HttpServletRequest request) throws DatastoreException {
		try {
			StringBuilder sb = new StringBuilder();
			InputStream is = request.getInputStream();
			int i = is.read();
			while (i>0) {
				sb.append((char)i);
				i = is.read();
			}
			is.close();
			return sb.toString();
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
