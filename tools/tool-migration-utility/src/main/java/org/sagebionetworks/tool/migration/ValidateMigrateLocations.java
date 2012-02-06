package org.sagebionetworks.tool.migration;

import java.io.IOException;
import java.util.List;
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
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;

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
public class ValidateMigrateLocations {
	
	static private Log log = LogFactory.getLog(ValidateMigrateLocations.class);
	static private Configuration configuration = new MigrationConfigurationImpl();
	
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
		((MigrationConfigurationImpl) configuration).loadConfigurationFile(path);
		
		AWSCredentials creds = new BasicAWSCredentials(args[1], args[2]);
		AmazonS3Client s3Client = new AmazonS3Client(creds);
		String bucketName = args[3];
		if(bucketName == null) throw new IllegalArgumentException("Bucket cannot be null");
		
		// Connect to the destination
		SynapseConnectionInfo destInfo = configuration.getDestinationConnectionInfo();
		SynapseConnectionInfo sourceInfo = configuration.getSourceConnectionInfo();
		log.info("Destination info: "+destInfo);
		
		ClientFactoryImpl factory = new ClientFactoryImpl();
		log.info("Connecting to repository: "+destInfo);
		Synapse destClient = factory.createNewConnection(destInfo);
		Synapse sourceClient = factory.createNewConnection(sourceInfo);

		// Create the query provider
		QueryRunner queryRunner = new QueryRunnerImpl(sourceClient);
		// Collect all location data.
		BasicProgress progress = new BasicProgress();

		log.info("Query for all location data: "+destInfo);
		List<EntityData> results = queryRunner.getAllEntityDataOfType(EntityType.location, progress);
		log.info("Found: "+results.size());
		progress.setTotal(results.size());
		for(int i=6800; i<results.size(); i++){
			EntityData locationData = results.get(i);
			
			// Now get the parent in the destination
			validate(locationData, sourceClient, destClient, bucketName, s3Client);
			// Give the other 
			progress.setCurrent(i);
			log.info("Processing: "+progress.getCurrentStatus().toStringHours());
			Thread.yield();
		}

	}
	
	public static void validate(EntityData locationData, Synapse sourceClient, Synapse destClient, String bucketName, AmazonS3Client s3Client) throws SynapseException{
		Location originalLocation = sourceClient.getEntity(locationData.getEntityId(), Location.class);
		// Now get the parent in the destination
		Locationable parent = (Locationable) destClient.getEntityById(locationData.getParentId());
		// Get the location data
		List<LocationData> locations = parent.getLocations();
		if(locations == null){
			log.error("parent.getLocations() returned null for parentId="+locationData.getParentId());
			return;
		}
		if(locations.size() != 1){
			// we are done, and this is a failure
			log.error("parent.getLocations().size() != 1 parentId="+locationData.getParentId());
			return;
		}
		LocationData location = locations.iterator().next();
		
		if(LocationTypeNames.external == originalLocation.getType()){
			// The path should match the oringal
			if(!originalLocation.getPath().equals(location.getPath())){
				log.error("Failed to set the new path."+locationData.getParentId()+" ");
				return;
			}
			if(!originalLocation.getMd5sum().equals(parent.getMd5())){
				parent.setMd5(originalLocation.getMd5sum());
				parent = destClient.putEntity(parent);
				if(!originalLocation.getMd5sum().equals(parent.getMd5())){
					log.error("Failed to migrate to new parent.  Expected MD5:"+originalLocation.getMd5sum()+" but was "+parent.getMd5()+" for parentId: "+parent.getId()+" locationId: "+locationData.getEntityId()+" type: "+originalLocation.getType());
					return;					
				}
			}
			
		}else if(LocationTypeNames.awss3 == originalLocation.getType()){
			// Extract the path
			String extractedPath = extractPath(originalLocation.getPath(), bucketName);
			// What should then new path be
			String newPath = MigrateLocations.calcualteNewPath(locationData.getEntityId(), locationData.getParentId(), extractedPath);
			// First get the metadata for the original file.
			try{
				ObjectMetadata originalMetadata = s3Client.getObjectMetadata(bucketName, extractedPath);

				String currentPath = MigrateLocations.extractPath(location.getPath(), bucketName);
				if(!currentPath.equals(newPath)){
					log.error("Failed to set the new path: "+locationData.getParentId());
					return;
				}
				if(!parent.getMd5().equals(originalMetadata.getETag())){
					log.error("Failed to migrate to new parent.  Expected MD5:"+originalMetadata.getETag()+" but was "+parent.getMd5());
					return;
				}
			}catch (AmazonClientException e){
				// Skip locations that do not exist
				log.error(e);
			}
		}else{
			throw new IllegalStateException("Unknown type "+originalLocation.getType());
		}
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
