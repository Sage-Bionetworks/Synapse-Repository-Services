package org.sagebionetworks.log.worker;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogKeyUtils;
import org.sagebionetworks.logging.s3.LogReader;

/**
 * A utility for creating log files.
 * @author John
 *
 */
public class LogTestUtils {
	
	/**
	 * Create a sample log with the given path and type.
	 * @param path
	 * @param type
	 * @param entries Each string will be treated as an entry
	 * @return
	 * @throws IOException
	 */
	public static File createSampleLogFile(String path, String type, String[] entries, long timestamp) throws IOException{
		File directory = new File(path);
		directory.mkdirs();
		File temp = new File(path, type+"."+UUID.randomUUID().toString()+".log.gz");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(temp))));
		try{
			// Write some log entries to the file
			for(int i=0; i<entries.length; i++){
				writer.write(LogKeyUtils.createISO8601GMTLogString(timestamp+i)+" "+entries[i]);
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
	
	/**
	 * Get all LogEntires from a reader.
	 * @param results
	 * @return
	 * @throws IOException
	 */
	public static List<LogEntry> getAllLogEntiresFromReader(LogReader results)
			throws IOException {
		List<LogEntry> list= new LinkedList<LogEntry>();
		LogEntry entry = null;
		do{
			entry = results.read();
			if(entry != null){
				list.add(entry);
			}
		}while(entry != null);
		return list;
	}
	
	/**
	 * Extract all of the {@link LogEntry} from the passed string.
	 * @param raw
	 * @return
	 * @throws IOException
	 */
	public static List<LogEntry> readLogEntries(String raw) throws IOException{
		LogReader results = new LogReader(new BufferedReader(new StringReader(raw)));
		return LogTestUtils.getAllLogEntiresFromReader(results);
	}

	/**
	 * Helper to create a log reader for some sample log data
	 * @param data
	 * @param timestamp
	 * @return
	 */
	public static LogReader createTestLogReader(String[] data, long timestamp){
		// Add each value to the log
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<data.length; i++){
			builder.append(LogKeyUtils.createISO8601GMTLogString(timestamp+i));
			builder.append(" ");
			builder.append(data[i]);
			builder.append("\n");
		}
		BufferedReader reader = new BufferedReader(new StringReader(builder.toString()));
		return new LogReader(reader);
	}
}
