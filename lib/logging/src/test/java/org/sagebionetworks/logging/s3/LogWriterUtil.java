package org.sagebionetworks.logging.s3;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * A utility for creating log files.
 * @author John
 *
 */
public class LogWriterUtil {

	/**
	 * Create a sample log with the given path and type.
	 * @param path
	 * @param type
	 * @param entries Each string will be treated as an entry
	 * @return
	 * @throws IOException
	 */
	public static File createSampleLogFile(String path, String type, String[] entries) throws IOException{
		long now = System.currentTimeMillis();
		File directory = new File(path);
		directory.mkdirs();
		File temp = new File(path, type+"."+UUID.randomUUID().toString()+".log.gz");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(temp))));
		try{
			// Write some log entries to the file
			for(String entry: entries){
				writer.write(LogKeyUtils.createISO8601GMTLogString(now)+" "+entry);
				writer.newLine();
			}
			return temp;
		}finally{
			try {
				writer.close();
			} catch (IOException e) {}
		}
	}
	
	/**
	 * Helper to stream all log entries for a given key
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public static List<LogEntry> streamEntiresForKey(LogReader reader) throws IOException{
		List<LogEntry> entries = new LinkedList<LogEntry>();
		assertNotNull(reader);
		try{
			LogEntry entry;
			do{
				entry = reader.read();
				if(entry != null){
					entries.add(entry);
				}
			}while(entry != null);
			return entries;
		}finally{
			reader.close();
		}
	}
}
