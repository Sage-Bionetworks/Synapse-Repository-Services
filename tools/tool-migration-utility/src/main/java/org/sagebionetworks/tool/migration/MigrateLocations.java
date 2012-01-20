package org.sagebionetworks.tool.migration;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.tool.migration.Progress.AggregateProgress;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;
import org.sagebionetworks.tool.migration.job.AWSInfo;
import org.sagebionetworks.tool.migration.job.LocationMergeWorker;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * Migrates locations.
 * @author John
 *
 */
public class MigrateLocations {
	
	static private Log log = LogFactory.getLog(MigrationDriver.class);
	/**
	 * Migrate all locations.
	 * @param args
	 * @throws IOException
	 * @throws SynapseException 
	 * @throws InterruptedException 
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws Exception {	
		if (args == null) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path.");
		}
		if (args.length < 4) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path.  [1]= BasicAWSCredentials.accessKey, [2]= BasicAWSCredentials.secretKey. [3]=bucketName args.length: "+args.length);
		}
		String path = args[0];
		// Load all of the configuration information.
		Configuration.loadConfigurationFile(path);
		
		AWSCredentials creds = new BasicAWSCredentials(args[1], args[2]);
		AmazonS3Client s3Client = new AmazonS3Client(creds);
		String bucketName = args[3];
		if(bucketName == null) throw new IllegalArgumentException("Bucket cannot be null");
		// The workers will use this data.
		AWSInfo awsInfo = new AWSInfo(creds, bucketName);
		
		// Connect to the destination
		SynapseConnectionInfo destInfo = Configuration.getDestinationConnectionInfo();
		log.info("Destination info: "+destInfo);
		
		ClientFactoryImpl factory = new ClientFactoryImpl();
		log.info("Connecting to respository: "+destInfo);
		Synapse destClient = factory.createNewConnection(destInfo);

		// Create the query provider
		QueryRunner queryRunner = new QueryRunnerImpl();
		// Collect all location data.
		BasicProgress progress = new BasicProgress();
		log.info("Query for all location data: "+destInfo);
		List<EntityData> results = queryRunner.getAllEntityDataOfType(destClient, EntityType.location, progress);
		log.info("Found: "+results.size());
		// Create a large thread pool
		AggregateProgress totalProgress = new AggregateProgress();
		ExecutorService threadPool = Executors.newFixedThreadPool(50);
		// Now process each one
		List<Future<Long>> futureList = new LinkedList<Future<Long>>();
		BasicProgress setupProgress = new BasicProgress();
		setupProgress.setCurrent(0);
		setupProgress.setTotal(results.size());
		for(int i=0; i<results.size(); i++){
			EntityData locationData = results.get(i);
			// Get the parent
			// Load this locations
			Location originalLocation = destClient.getEntity(locationData.getEntityId(), Location.class);
			// Get the path
			setupProgress.setCurrent(i);
			log.info("Processing: "+setupProgress.getCurrentStatus().toStringHours()+" for path: "+originalLocation.getPath());
			if(LocationTypeNames.external == originalLocation.getType()){
				// Skip this for now
				String nonS3LocationPath = originalLocation.getPath();
				Locationable parent = (Locationable) destClient.getEntityById(originalLocation.getParentId());
				// Now check the location.
				List<LocationData> locations = parent.getLocations();
				if(locations == null) {
					log.error("Could not migrate location: "+locationData.getEntityId()+" because its parent: "+originalLocation.getParentId()+" did not have any location data.  Path: "+originalLocation.getPath());
					continue;
					//throw new IllegalStateException("Locations null for "+locationData);
				}
				if(locations.size() != 1){
					// we are done, and this is a failure
					progress.setDone();
					throw new IllegalStateException("There is more than one location for this entity: "+originalLocation.getParentId());
					
				}
				LocationData location = locations.iterator().next();
				// Check the path
				if(!nonS3LocationPath.equals(location.getPath())){
					throw new IllegalStateException("The location should already be set!");
				}
				// Push the change to make in permanent.
				Locationable updated = destClient.putEntity(parent);
			}else if(LocationTypeNames.awss3 == originalLocation.getType()){
				// Extract the path
				String extractedPath = extractPath(originalLocation.getPath(), bucketName);
				// First get the metadata for the original file.
				try{
					ObjectMetadata originalMetadata = s3Client.getObjectMetadata(bucketName, extractedPath);
					// Create a worker
					BasicProgress workerProgress = new BasicProgress();
					totalProgress.addProgresss(workerProgress);
					LocationMergeWorker worker = new LocationMergeWorker(destInfo, awsInfo , originalLocation, originalMetadata, workerProgress);
					// Get to work
					futureList.add(threadPool.submit(worker));
				}catch (AmazonClientException e){
					log.error(e);
					continue;
				}
			}else{
				throw new IllegalStateException("Unknown type "+originalLocation.getType());
			}
			// Delete the location now that it is migrated.
			destClient.deleteEntity(originalLocation);

		}
		// Now add all of the workers to the pool
		// Wait for all threads to finish
		while(isStillWorking(futureList)){
			// Update the log
			log.info("Processing S3 Object: "+totalProgress.getCurrentStatus().toStringHours());
			Thread.sleep(2000);
		}
		log.info("Finished: "+totalProgress.getCurrentStatus().toStringHours());
		threadPool.shutdown();
	}
	
	/**
	 * Find any thread still working
	 * @param futureList
	 * @return
	 */
	private static boolean isStillWorking(List<Future<Long>> futureList){
		// Check to see if all workers are still working
		for(Future<Long> future: futureList){
			if(!future.isDone()){
				return true;
			}
		}
		// Everybody is done
		return false;
	}
	
	/**
	 * Extract the path
	 * @param path
	 * @return
	 */
	public static String extractPath(String path, String bucket){
		if(path == null) throw new IllegalArgumentException("Path cannot be null");
		String[] split = path.split("\\?");
		split = split[0].split(bucket);
		return split[1].substring(1, split[1].length());
	}
	
	/**
	 * Given the locationId, parentId and currentPath what is the expected new path?
	 * @param locationId
	 * @param parentId
	 * @param extacted
	 * @return
	 */
	public static String calcualteNewPath(String locationId, String parentId, String extacted){
		String result = extacted.replaceFirst(locationId, parentId);
		return result.replaceFirst("0.0.0", locationId);
	}
	

}
