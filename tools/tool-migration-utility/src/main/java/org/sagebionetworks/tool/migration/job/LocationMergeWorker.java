package org.sagebionetworks.tool.migration.job;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.tool.migration.ClientFactoryImpl;
import org.sagebionetworks.tool.migration.MigrateLocations;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * Migrates a single location.
 * 
 * @author John
 *
 */
public class LocationMergeWorker implements Callable<Long> {
	
	SynapseConnectionInfo destInfo;
	AWSInfo awsInfo;
	Location locationToMigrate;
	private BasicProgress progress;
	ObjectMetadata originalMetadata;



	public LocationMergeWorker(SynapseConnectionInfo destInfo, AWSInfo awsInfo,	Location locationToMigrate, ObjectMetadata original, BasicProgress progress) {
		super();
		this.destInfo = destInfo;
		this.awsInfo = awsInfo;
		this.locationToMigrate = locationToMigrate;
		this.progress = progress;
		// Setup the progress from the metadata
		this.originalMetadata = original;
		progress.setCurrent(0);
		progress.setTotal(original.getContentLength());
	}




	@Override
	public Long call() throws Exception {
		// First create an S3 Client
		AmazonS3Client s3Client = new AmazonS3Client(awsInfo.getCredentials());
		// Now fetch the location data
		String extractedPath = MigrateLocations.extractPath(locationToMigrate.getPath(), awsInfo.getBucket());
		// What should then new path be
		String newPath = MigrateLocations.calcualteNewPath(locationToMigrate.getId(), locationToMigrate.getParentId(), extractedPath);
		// First get the metadata for the original file.
		try{
			ObjectMetadata meta = s3Client.getObjectMetadata(awsInfo.getBucket(), newPath);
			// The file exists but does it match the current etag
			if(!meta.getETag().equals(originalMetadata.getETag())){
				// The etags do not match
				// First delete the new object
				s3Client.deleteObject(awsInfo.getBucket(), newPath);
				// Now copy the old to the new.
				// the new file does not exist so create it by copying the original
				CopyObjectResult cor = s3Client.copyObject(awsInfo.getBucket(), extractedPath, awsInfo.getBucket(), newPath);
			}
		}catch(AmazonClientException e){
			// We get a forbidden error when an object does not exist.
			if(!"Forbidden".equals(e.getMessage())){
				// Something else is wrong.
				throw e;
			}else{
				// the new file does not exist so create it by copying the original
				CopyObjectResult cor = s3Client.copyObject(awsInfo.getBucket(), extractedPath, awsInfo.getBucket(), newPath);
			}
		}
		// Now we can update the parent entity
		ClientFactoryImpl factory = new ClientFactoryImpl();
		Synapse destClient = factory.createNewConnection(destInfo);
		Locationable parent = (Locationable) destClient.getEntityById(locationToMigrate.getParentId());
		// Now check the location.
		List<LocationData> locations = parent.getLocations();
		if(locations.size() != 1){
			// we are done, and this is a failure
			progress.setDone();
			throw new IllegalStateException("There is more than one location for this entity: "+locationToMigrate.getParentId());
			
		}
		LocationData location = locations.iterator().next();
		String currentPath = MigrateLocations.extractPath(location.getPath(), awsInfo.getBucket());
		// Check the path
		if(!newPath.equals(currentPath)){
			// Set the new path for this entity.
			location.setPath("/"+newPath);
			// Update the entity
			Locationable updated = destClient.putEntity(parent);
			// Validate the new path
			String roundTripPath = updated.getLocations().iterator().next().getPath();
			String newRoundPath = MigrateLocations.extractPath(roundTripPath, awsInfo.getBucket());
			if(!newPath.equals(newRoundPath)){
				throw new IllegalStateException("Failed to migrate location "+locationToMigrate.getId()+" parent path="+newRoundPath+" but should be: "+newPath);
			}
		}
		// Now that we are done set the progress to done
		progress.setDone();
		return progress.getTotal();
	}
	

}
