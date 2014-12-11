package org.sagebionetworks.util;

import java.io.Closeable;
import java.io.IOException;

public class Closer {
	public static void closeQuietly(Closeable... closeables) {
		for (Closeable closeable : closeables) {
			try {
				if (closeable != null) {
					closeable.close();
				}
			} catch (IOException e) {
			}
		}
	}
}
