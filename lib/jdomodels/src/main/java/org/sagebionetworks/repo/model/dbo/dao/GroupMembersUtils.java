package org.sagebionetworks.repo.model.dbo.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GroupMembersUtils {

	/**
	 * Gzips the given list. 
	 * @param longs Each element must be parsable as a long
	 * @return A gzipped byte array of long ints 
	 */
	public static byte[] zip(List<String> longs) throws IOException {
		// Convert the Strings into Longs into Bytes
		ByteBuffer converter = ByteBuffer.allocate(Long.SIZE / 8 * longs.size());
		for (int i = 0; i < longs.size(); i++) {
			converter.putLong(Long.parseLong(longs.get(i)));
		}
		
		// Zip up the bytes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipped = new GZIPOutputStream(out);
		zipped.write(converter.array());
		zipped.flush();
		zipped.close();
		return out.toByteArray();
	}
	
	/**
	 * Un-gzips the given array.  
	 * @param zippedLongs Gzipped array of long ints
	 * @return A list of longs in base 10 string form
	 */
	public static List<String> unzip(byte[] zippedLongs) throws IOException {
		// Unzip the bytes
		ByteArrayInputStream in = new ByteArrayInputStream(zippedLongs);
		GZIPInputStream unzip = new GZIPInputStream(in);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (unzip.available() > 0) {
			out.write(unzip.read());
		}
		
		// Convert to bytes
		ByteBuffer converter = ByteBuffer.wrap(out.toByteArray());
		LongBuffer converted = converter.asLongBuffer();
		List<String> verbose = new ArrayList<String>();
		while (converted.hasRemaining()) {
			verbose.add(Long.toString(converted.get()));
		}
		return verbose;
	}
}
