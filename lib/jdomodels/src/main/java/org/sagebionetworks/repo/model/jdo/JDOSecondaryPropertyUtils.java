package org.sagebionetworks.repo.model.jdo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.UnmodifiableXStream;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOSecondaryPropertyUtils {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Compresses the dto into compressed XML bytes using the provided customXStream
	 * @param customXStream a UnmodifiableXStream that has been set up to handle the object type
	 * @param dto the object to be serialized and compressed
	 * @return byte[] of compressed XML representing the dto object.
	 * @throws IOException
	 */
	public static byte[] compressObject(UnmodifiableXStream customXStream, Object dto) throws IOException {
		if(dto == null) return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipper = new GZIPOutputStream(out);
		try(Writer zipWriter = new OutputStreamWriter(zipper, UTF8);){
			customXStream.toXML(dto, zipWriter);
		}
		return out.toByteArray();
	}

	/**
	 * Decompress and deserialize compressed XML bytes into an object using the provided customXStream
	 * @param customXStream a UnmodifiableXStream that has been set up to handle the object type
	 * @param zippedBytes byte[] of compressed XML representing the dto object.
	 * @return the object that the bytes represented
	 * @throws IOException
	 */
	public static Object decompressObject(UnmodifiableXStream customXStream, byte[] zippedBytes) throws IOException{
		if(zippedBytes == null){
			return null;
		}

		ByteArrayInputStream in = new ByteArrayInputStream(zippedBytes);
		GZIPInputStream unZipper = new GZIPInputStream(in);
		try(Reader reader = new InputStreamReader(unZipper, UTF8);){
			return customXStream.fromXML(unZipper);
		}
	}
}
