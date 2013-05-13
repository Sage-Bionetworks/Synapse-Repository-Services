package org.sagebionetworks.repo.model.migration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MigrationUtils {

	private static Long NULL_ID = -1L;
	/**
	 * Bucket tree vertices by level in the tree.  The first bucket will contain vertices with either null parent
	 * or parents that are not included in the input.  The nth bucket will contain vertices where the their parent 
	 * vertices is in bucket n-1.  The last bucket will contain leaf vertices.
	 * @param in
	 * @param appender
	 */
	public static void bucketByTreeLevel(Iterator<RowMetadata> input, BucketProvider<Long> provider){
		// Build up a map of all data
		Set<Long> inputIdSet = new HashSet<Long>();
		// Build a map of parent ID to their children.
		Map<Long, List<RowMetadata>> parentToChildren = new LinkedHashMap<Long, List<RowMetadata>>();
		while(input.hasNext()){
			RowMetadata row = input.next();
			inputIdSet.add(row.getId());
			Long parentId = row.getParentId();
			if(parentId == null){
				parentId = NULL_ID;
			}
			// Add this to the map
			List<RowMetadata> children = parentToChildren.get(parentId);
			if(children == null){
				children = new LinkedList<RowMetadata>();
				parentToChildren.put(parentId, children);
			}
			children.add(row);
		}
		Set<Long> added = new HashSet<Long>();
		// fist pass is to find roots
		Iterator<Entry<Long, List<RowMetadata>>> it = parentToChildren.entrySet().iterator();
		while(it.hasNext()){
			Entry<Long, List<RowMetadata>> entry = it.next();
			// We can write this list if the following conditions are true.
			// The parent ID is null or the Parent ID is not in the map.
			Long parentId = entry.getKey();
			if(parentId.equals(NULL_ID) || !inputIdSet.contains(parentId)){
				// This is a root so add it to the set
				added.add(parentId);
			}
		}
		// If added is still empty then then there are no roots
		if(added.isEmpty()) throw new IllegalArgumentException("Did not find any roots");
		
		// Now walk parentToChildMap in breadth fist
		do{
			// Create a new bucket
			Bucket<Long> bucket = provider.newBucket();
			Set<Long> processedParents = new HashSet<Long>(2);
			Set<Long> processedChildren = new HashSet<Long>(2);
			it = parentToChildren.entrySet().iterator();
			while(it.hasNext()){
				Entry<Long, List<RowMetadata>> entry = it.next();
				// We can write this list if the following conditions are true.
				// The parent ID is null or the Parent ID is not in the map.
				Long parentId = entry.getKey();
				List<RowMetadata> children = entry.getValue();
				// We can add these children if there parent has been added.
				if(added.contains(parentId)){
					for(RowMetadata row: children){
						bucket.append(row.getId());
						processedChildren.add(row.getId());
					}
					processedParents.add(parentId);
				}
			}
			// Now remove all parents that have been added
			Iterator<Long> removeIt = processedParents.iterator();
			while(removeIt.hasNext()){
				parentToChildren.remove(removeIt.next());
			}
			added.addAll(processedChildren);
		}while(!parentToChildren.isEmpty());
		
	}
}
