package org.sagebionetworks.log.worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.logging.s3.LogDAO;
import org.sagebionetworks.logging.s3.LogKeyUtils;
import org.sagebionetworks.logging.s3.LogReader;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This worker collates all logs based on log type and time.
 * 
 * @author John
 *
 */
public class LogCollateWorker {
		
	static private Logger log = LogManager.getLogger(LogCollateWorker.class);
	private LogDAO logDAO;

	/**
	 * All dependencies are provide at construction time.
	 * @param logDAO
	 */
	public LogCollateWorker(LogDAO logDAO) {
		super();
		this.logDAO = logDAO;
	}

	/**
	 * Merge one batch of files.
	 * 
	 * @return True if there are more files to be merged, else False.
	 */
	public boolean mergeOneBatch() {
		try {
			// Walk all of the logs looking for multiple logs with the same type/date/hour
			String marker = null;
			BatchData batchData = null;
			do{
				ObjectListing listing = logDAO.listAllStackInstanceLogs(marker);
				marker = listing.getNextMarker();
				if(listing.getObjectSummaries() != null){
					for(S3ObjectSummary summ: listing.getObjectSummaries()){
						// Bucket by type/date/hour
						String typeDateHour = LogKeyUtils.getTypeDateAndHourFromKey(summ.getKey());
						// Do we have a batch?
						if (batchData != null) {
							// The current batch is completed if the next file
							// does not have the same type/date/hour as the current
							// batch.
							if (!batchData.batchDateString.equals(typeDateHour)) {
								// The next log does not belong in the current batch so
								collateBatch(batchData);
								// We are done with this batch
								batchData = null;
							}
						}
							
						// If the batchData is null then this is the start
						// of a new batch.
						if (batchData == null) {
							batchData = new BatchData(new LinkedList<String>(),	typeDateHour);
						}
						// add this file to the current batch
						batchData.mergedKeys.add(summ.getKey());
					}
				}
			}while(marker != null);
			// If there is any batch data left then complete it.
			if(batchData != null){
				collateBatch(batchData);
			}
			// If we made it this far then there is no more data.
			return false;
		} catch (Exception e) {
			log.error("Worker failed", e);
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * This will save the new batch file and delete all of the sub files that were merged.
	 * @param data
	 * @return True if the batch was merged.  False if there was nothing to merge or the batch was empty.
	 */
	private boolean collateBatch(BatchData data) {
		if(data != null){
			if(data.mergedKeys.size() > 0){
				// If this batch only contains one file there is nothing to do.
				if(data.mergedKeys.size() < 2){
					return false;
				}
				try {
					// Create the file that will contain the collated data.
					String type = LogKeyUtils.getTypeFromTypeDateHour(data.batchDateString);
					// This timestamp will be used to create the key of the resulting output file.
					long timestamp = LogKeyUtils.getTimestampFromTypeDateHour(data.batchDateString);
					String newFileKey = null;
					File temp = File.createTempFile(type+".", ".log.gz");
					BufferedWriter outWriter = null;
					LogReader[] toCollate = new LogReader[data.mergedKeys.size()];
					File[] tempFiles = new File[data.mergedKeys.size()];
					try{
						// Now setup the writer
						outWriter = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(temp))));
						// Get the log reader for each file to collate
						int index = 0;
						for(String key: data.mergedKeys){
							tempFiles[index] = File.createTempFile("collateDownload", ".log.gz");
							ObjectMetadata meta = logDAO.downloadLogFile(key, tempFiles[index]);
							toCollate[index] = new LogReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(tempFiles[index])))));
							index++;
						}
						// Now collate all of the files
						CollateUtils.collateLogs(toCollate, outWriter);
						// Flush and close the outupt files before we save it
						outWriter.flush();
						outWriter.close();
						// Save the results back to s3
						newFileKey = logDAO.saveLogFile(temp, timestamp);
					}finally{
						// Close the output stream
						if(outWriter != null){
							try {
								outWriter.close();
							} catch (Exception e) {}
						}
						// Close the readers.
						for(LogReader reader: toCollate){
							try {
								reader.close();
							} catch (Exception e) {}
						}
						// Delete all temp files
						for(File tempIn: tempFiles){
							tempIn.delete();
						}
						// We are done with the temp file.
						temp.delete();
					}
					
					// Now delete all of the files that were merged.
					for(String key: data.mergedKeys){
						logDAO.deleteLogFile(key);
					}
					long elapse = System.currentTimeMillis()-data.startMs;
					long msPerfile = elapse/data.mergedKeys.size();
					log.info("Merged: "+data.mergedKeys.size()+" files into new file: "+newFileKey+" in "+elapse+" ms rate of: "+msPerfile+" ms/file");
					// We merged this batch successfully
					return true;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return false;
	}
	
	/**
	 * All of the data about a batch.
	 *
	 */
	private static class BatchData {
		List<String> mergedKeys = null;
		String batchDateString;
		long startMs;
		public BatchData(List<String> mergedKeys, String batchDateString) {
			super();
			this.mergedKeys = mergedKeys;
			this.batchDateString = batchDateString;
			this.startMs = System.currentTimeMillis();
		}
	}

}
