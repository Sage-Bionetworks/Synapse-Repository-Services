package org.sagebionetworks.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

public class ZipUtils {
	public static byte[] unzip(byte[] zippedBytes) throws IOException {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(zippedBytes);
		     GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
		     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			IOUtils.copy(gzipInputStream, outputStream);
			return outputStream.toByteArray();
		}
	}

	public static byte[] zip(byte[] unzippedBytes) throws IOException {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		     GZIPOutputStream gzip = new GZIPOutputStream(outputStream)
		) {
			gzip.write(unzippedBytes);
			gzip.finish();
			return outputStream.toByteArray();
		}
	}
}
