package org.sagebionetworks.repo.model.migration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * List backed Bucket provider.
 * 
 * @author John
 *
 */
public class ListBucketProvider implements BucketProvider<Long>{
	
	List<List<Long>> listOfBuckets = new ArrayList<List<Long>>();

	@Override
	public Bucket<Long> newBucket() {
		final List<Long> bucket = new LinkedList<Long>();
		listOfBuckets.add(bucket);
		return new Bucket<Long>(){
			@Override
			public void append(Long toAppend) {
				bucket.add(toAppend);
			}};
	}

	/**
	 * The list of buckets provided by this object
	 * @return
	 */
	public List<List<Long>> getListOfBuckets() {
		return listOfBuckets;
	}

}
