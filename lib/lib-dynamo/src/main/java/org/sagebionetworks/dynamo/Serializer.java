package org.sagebionetworks.dynamo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.thoughtworks.xstream.XStream;

public class Serializer {

	public static byte[] compressObject(Object objectToSerialize) throws IOException {
		if (objectToSerialize == null)
			return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		try {
			XStream xstream = new XStream();
			xstream.toXML(objectToSerialize, zipper);
			zipper.flush();
			zipper.close();
			return out.toByteArray();
		} finally {
			zipper.close();
		}
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
