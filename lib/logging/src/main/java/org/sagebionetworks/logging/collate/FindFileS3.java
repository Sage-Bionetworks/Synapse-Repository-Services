package org.sagebionetworks.logging.collate;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class FindFileS3 {

	private AmazonS3 s3Client;
	
	public void checkS3Bucket() {
		AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAIMSZKADLIESJW47A", "egIf9b3JJ+epHWRfrhn0uzWB2cD2FQGWaSdqkJua");
		s3Client = new AmazonS3Client(awsCredentials);
		
		ListObjectsRequest request = new ListObjectsRequest().withBucketName("logs.sagebase.org")
				.withPrefix("prod/A/")
				.withDelimiter("/");

		ObjectListing listing;
		do {
			listing = s3Client.listObjects(request);
			for (S3ObjectSummary objectSummary : listing
					.getObjectSummaries()) {
				System.out.println(" - " + objectSummary.getKey() + "  "
						+ "(size = " + objectSummary.getSize() + ")");
			}
			request.setMarker(listing.getNextMarker());
		} while (listing.isTruncated());
	}
}
