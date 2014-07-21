package org.sagebionetworks.dynamo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.util.Closer;

import com.thoughtworks.xstream.XStream;

public class Serializer {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static byte[] compressObject(Object objectToSerialize) throws IOException {
		if (objectToSerialize == null)
			return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		Writer zipWriter = new OutputStreamWriter(zipper, UTF8);
		try {
			XStream xstream = new XStream();
			xstream.toXML(objectToSerialize, zipWriter);
		} finally {
			Closer.closeQuietly(zipWriter, zipper, buff, out);
		}
		return out.toByteArray();
	}

	public static Object decompressedObject(byte[] zippedBytes) throws IOException {
		if (zippedBytes == null) {
			return null;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(zippedBytes);
		GZIPInputStream unZipper = new GZIPInputStream(in);
		try {
			XStream xstream = new XStream();
			return xstream.fromXML(unZipper);
		} finally {
			unZipper.close();
		}
	}
}
