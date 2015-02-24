package org.sagebionetworks.downloadtools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for working with files.
 * 
 * @author John
 *
 */
public class FileUtils {
	
	/**
	 * Chunk a file into smaller files each with a size <= chunkSize.
	 * 
	 * @param file
	 * @param chunkSize - The maximum size of a single chunk
	 * @return
	 * @throws IOException 
	 */
	public static List<File> chunkFile(File file, long chunkSize) throws IOException{
		List<File> results = new LinkedList<File>();
		if(file.length() <= chunkSize){
			// No need to chunk the file as it is already of <= chunkSize
			results.add(file);
		}else{
			// The file is larger than the chunk size so we will create a temp file for each chunk
			BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
			try{
				// We need to chunk the file
				int plus = file.length()%chunkSize > 0 ? 1 : 0;
				int chunkCount = (int) (file.length()/chunkSize + plus);
				for(int i=0; i<chunkCount; i++){
					// Create a new file
					File temp = File.createTempFile("FileUtilsChunkFile", ".tmp");
					BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(temp));
					try{
						// Write the chunk to a buffer
						long start = i*chunkSize;
						long end = start+chunkSize;
						long index = start;
						int read =0;
						while(index < end && read > -1){
							// Since both streams are buffered we just read/write one byte at a time
							read = fis.read();
							if(read > -1){
								fos.write(read);
							}
							index++;
						}
						// Add the file to the results
						results.add(temp);
					}catch(Throwable e){
						// Unconditionally close the stream
						try{fos.close();}catch(Throwable nb){};
						// delete the temp file
						temp.delete();
						throw new IOException(e);
					}finally{
						fos.close();
					}
				}
			}catch(Throwable e){
				// If anything goes wrong we need to delete any files that were created
				for(File temp: results){
					temp.delete();
				}
				throw new IOException(e);
			}finally{
				fis.close();
			}
		}
		return results;
	}
	
	/**
	 * Delete all of the files on the passed list excluding the exception file.
	 * @param exception
	 * @param list
	 */
	public static void deleteAllFilesExcludingException(File exception, List<File> toDelete){
		if(exception == null) throw new IllegalArgumentException("exception cannot be null");
		// If any temp files were created we need to delte them
		if(toDelete != null){
			// Delete any file that is not the original file
			for(File file: toDelete){
				if(!exception.equals(file)){
					file.delete();
				}
			}
		}
	}

	/**
	 * Write a string compressed to an output stream.
	 * @param content
	 * @param out
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static void writeCompressedString(String content, OutputStream out) throws IOException,
			UnsupportedEncodingException {
		GZIPOutputStream gzout = null;
		OutputStreamWriter outw = null;
		try {
			gzout = new GZIPOutputStream(out);
			outw = new OutputStreamWriter(gzout, "UTF-8");
			outw.append(content);
			outw.flush();
			outw.close();
		} finally {
			gzout.close();
			out.close();
		}
	}
	
	/**
	 * Read compressed data from file as a string.
	 */
	public static String readCompressedStreamAsString(InputStream in) throws IOException {
		GZIPInputStream gzin = new GZIPInputStream(in);
		BufferedInputStream bis = new BufferedInputStream(gzin);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int read = -1;
			while((read = bis.read(buffer)) > 0){
				baos.write(buffer, 0, read);
			}
		} finally {
			baos.close();
			bis.close();
			gzin.close();
			in.close();
		}
		String fromZip = new String(baos.toByteArray(), "UTF-8");
		return fromZip;
	}
}
