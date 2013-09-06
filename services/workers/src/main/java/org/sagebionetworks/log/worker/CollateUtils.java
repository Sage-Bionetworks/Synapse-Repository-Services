package org.sagebionetworks.log.worker;

import java.io.BufferedWriter;
import java.io.IOException;

import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogReader;

public class CollateUtils {

	/**
	 * Collate all of the log data from the list of LogReaders into the passed writer
	 * 
	 * @param toCollate
	 * @param out
	 * @throws IOException 
	 */
	public static void collateLogs(LogReader[] toCollate, BufferedWriter out) throws IOException{
		// First find the
		int minIndex;
		LogEntry[] tail = new LogEntry[toCollate.length];
		// prime the pump
		for(int i=0; i<toCollate.length; i++){
			tail[i] = toCollate[i].read();
		}
		LogEntry toWrite = null;
		do{
			// Find the minimum;
			minIndex = 0;
			for(int i=1; i<tail.length; i++){
				if(tail[minIndex] == null){
					minIndex = i;
					continue;
				}
				if(tail[i] == null){
					continue;
				}
				if(tail[i].getTimeStamp() < tail[minIndex].getTimeStamp()){
					minIndex = i;
				}
			}
			// write the min
			toWrite = tail[minIndex];
			if(toWrite != null){
				out.write(toWrite.getEntryString());
				out.newLine();
				// Get the next from this reader
				tail[minIndex] = toCollate[minIndex].read();
			}
		}while(toWrite != null);
	}
}
