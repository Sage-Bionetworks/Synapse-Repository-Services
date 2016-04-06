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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.logging.s3.LogDAO;
import org.sagebionetworks.logging.s3.LogKeyUtils;
import org.sagebionetworks.logging.s3.LogReader;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This worker collates all logs based on log type and time.
 * 
 * @author John
 *
 */
public class LogCollateWorker implements ProgressingRunner<Void>{
		
	static private Logger log = LogManager.getLogger(LogCollateWorker.class);

	@Autowired
	LogDAO logDAO;


	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		try {
			// Walk all of the logs looking for multiple logs with the same type/date/hour
			String marker = null;
			BatchData batchData = null;
			do{
				ObjectListing listing = logDAO.listAllStackInstanceLogs(marker);
				marker = listing.getNextMarker();
				if(listing.getObjectSummaries() != null){
					for(S3ObjectSummary summ: listing.getObjectSummaries()){
						progressCallback.progressMade(null);
						// Bucket by type/date/hour
						String typeDateHour = LogKeyUtils.getTypeDateAndHourFromKey(summ.getKey());
						// Do we have a batch?
						if (batchData != null) {
							// The current batch is completed if the next file
							// does not have the same type/date/hour as the current
							// batch.
							if (!batchData.batchDateString.equals(typeDateHour)) {
								// The next log does not belong in the current batch so
								collateBatch(batchData, progressCallback);
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
				collateBatch(batchData, progressCallback);
			}
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
	private void collateBatch(BatchData data, ProgressCallback<Void> progressCallback) {
		if(data != null){
			if(data.mergedKeys.size() > 0){
				// If this batch only contains one file there is nothing to do.
				if(data.mergedKeys.size() < 2){
					return;
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
							progressCallback.progressMade(null);
						}
						// Now collate all of the files
						CollateUtils.collateLogs(toCollate, outWriter, progressCallback);
						// Flush and close the outupt files before we save it
						outWriter.flush();
						outWriter.close();
						// Save the results back to s3
						newFileKey = logDAO.saveLogFile(temp, timestamp);
					}catch(Exception e){
						log.error("Worker failed", e);
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
								if(reader != null){
									reader.close();
								}
							} catch (Exception e) {}
						}
						// Delete all temp files
						for(File tempIn: tempFiles){
							try {
								if(tempIn != null){
									tempIn.delete();
								}
							} catch (Exception e) {}
						}
						// We are done with the temp file.
						try {
							if(temp != null){
								temp.delete();
							}
						} catch (Exception e) {}
					}
					
					// Now delete all of the files that were merged.
					for(String key: data.mergedKeys){
						logDAO.deleteLogFile(key);
					}
					long elapse = System.currentTimeMillis()-data.startMs;
					long msPerfile = elapse/data.mergedKeys.size();
					if(log.isTraceEnabled()){
						log.trace("Merged: "+data.mergedKeys.size()+" files into new file: "+newFileKey+" in "+elapse+" ms rate of: "+msPerfile+" ms/file");
					}
				} catch (IOException e) {
					log.error("Worker failed", e);
				}
			}
		}
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
