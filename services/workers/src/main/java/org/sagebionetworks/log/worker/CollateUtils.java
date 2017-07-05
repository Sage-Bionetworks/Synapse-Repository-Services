package org.sagebionetworks.log.worker;

import java.io.BufferedWriter;
import java.io.IOException;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogReader;

/**
 * Simple utility for Collating logs by streaming the data
 * 
 * @author jmhill
 *
 */
public class CollateUtils {

	/**
	 * Collate all of the log data from the list of LogReaders into the passed writer
	 * @param toCollate
	 * @param out
	 * @throws IOException 
	 */
	public static void collateLogs(LogReader[] toCollate, BufferedWriter out, ProgressCallback progressCallback) throws IOException{
		// First find the
		int minIndex;
		// This array will contain the head entry from each input log.
		LogEntry[] heads = new LogEntry[toCollate.length];
		// prime the pump by reading in the head from each log.
		for(int i=0; i<toCollate.length; i++){
			heads[i] = toCollate[i].read();
		}
		// Now find the earliest log entry from all of the heads
		// and write it to the output.
		LogEntry toWrite = null;
		do{
			// Find the minimum;
			minIndex = findMiniumnIndex(heads, progressCallback);
			// write the entry at the minimum index to the log
			toWrite = heads[minIndex];
			if(toWrite != null){
				// write the entry at the minimum index.
				out.write(toWrite.getEntryString());
				out.newLine();
				// Now read the next entry the minimum log
				heads[minIndex] = toCollate[minIndex].read();
			}
			// continue until there is nothing else to write.
		}while(toWrite != null);
	}
	/**
	 * Look at all of the current heads from the input log files and find the one
	 * with the minimum time stamp
	 * @param heads
	 * @return The index of LogEntry that has the earliest time stamp.
	 * If the passed array contains all null values then zero will be returned.
	 */
	private static int findMiniumnIndex(LogEntry[] heads, ProgressCallback progressCallback){
		int minIndex = 0;
		for(int i=1; i<heads.length; i++){
			// If data is null at the current minIndex
			// this this index becomes the minimum.
			if(heads[minIndex] == null){
				minIndex = i;
				continue;
			}
			// There is data at the minimum index
			// but is there data at this index?
			if(heads[i] == null){
				// There is no data at this index so it cannot be the minimum.
				continue;
			}
			// There is data both at this index and the current minimum
			// so determine which occurred earlier.
			if(heads[i].getTimeStamp() < heads[minIndex].getTimeStamp()){
				// This current index becomes the new minimum.
				minIndex = i;
			}
		}
		return minIndex;
	}
}
