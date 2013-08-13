package org.sagebionetworks.repo.manager.audit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.audit.ObjectCSVReader;
import org.sagebionetworks.audit.ObjectCSVWriter;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.BatchListing;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * S3 implementation of the AccessRecordManager.
 * 
 * @author jmhill
 *
 */
public class AccessRecordManagerImpl implements AccessRecordManager {
	
	static private Log log = LogFactory.getLog(S3AccessRecorder.class);
	
	@Autowired
	private AmazonS3Client s3Client;
	@Autowired
	private String bucketName;
	
	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize(){
		if(bucketName == null) throw new IllegalArgumentException("bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(bucketName);
	}

	@Override
	public String saveBatch(List<AccessRecord> batch) {
		try{
			// We are now free to process the current batch with out synchronization or data loss. 
			// Order the batch by timestamp
			sortByTimestamp(batch);
			// Write the data to a gzip
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream zipOut = new GZIPOutputStream(out);
			OutputStreamWriter osw = new OutputStreamWriter(zipOut);
			ObjectCSVWriter<AccessRecord>  writer = new ObjectCSVWriter<AccessRecord>(osw, AccessRecord.class);
			// Write all of the data
			for(AccessRecord ar: batch){
				writer.append(ar);
			}
			writer.close();
			// Create an input stream
			byte[] bytes = out.toByteArray();
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			String key = UUID.randomUUID().toString()+".csv.gz";
			ObjectMetadata om = new ObjectMetadata();
			om.setContentType("application/x-gzip");
			om.setContentEncoding("gzip");
			om.setContentDisposition("attachment; filename="+key+";");
			om.setContentLength(bytes.length);
			s3Client.putObject(bucketName, key, in, om);
			return key;
		}catch(Exception e){
			log.error("Failed to write batch", e);
			return null;
		}
	}
	
	@Override
	public List<AccessRecord> getSavedBatch(String key) throws IOException {
		// Attempt to download the object
		S3Object object = s3Client.getObject(bucketName, key);
		InputStream input = object.getObjectContent();
		try{
			// Read the data
			GZIPInputStream zipIn = new GZIPInputStream(input);
			InputStreamReader isr = new InputStreamReader(zipIn);
			ObjectCSVReader<AccessRecord> reader = new ObjectCSVReader<AccessRecord>(isr, AccessRecord.class);
			List<AccessRecord> results = new LinkedList<AccessRecord>();
			AccessRecord record = null;
			while((record = reader.next()) != null){
				results.add(record);
			}
			reader.close();
			return results;
		}finally{
			if(input != null){
				input.close();
			}
		}
	}
	

	
	/**
	 * This Comparator compares AccessRecord based on the time stamp.
	 * 
	 * @author jmhill
	 * 
	 */
	public static class AccessRecordComparator implements
			Comparator<AccessRecord> {
		@Override
		public int compare(AccessRecord one, AccessRecord two) {
			if (one == null)
				throw new IllegalArgumentException("One cannot be null");
			if (one.getTimestamp() == null)
				throw new IllegalArgumentException(
						"One.timestamp cannot be null");
			if (two == null)
				throw new IllegalArgumentException("Two cannot be null");
			if (two.getTimestamp() == null)
				throw new IllegalArgumentException(
						"Two.timestamp cannot be null");
			return one.getTimestamp().compareTo(two.getTimestamp());
		}
	}

	/**
	 * Sort the list of AccessRecord based on timestamp
	 * @param toSort
	 */
	public static void sortByTimestamp(List<AccessRecord> toSort){
		Collections.sort(toSort, new AccessRecordComparator());
	}

	@Override
	public BatchListing listBatchKeys(String marker) {
		// TODO Auto-generated method stub
		return null;
	}

}
