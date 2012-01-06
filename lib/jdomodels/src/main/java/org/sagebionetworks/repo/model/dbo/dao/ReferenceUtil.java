package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Converts reference to their DBO object.
 * @author John
 *
 */
public class ReferenceUtil {
	
	/**
	 * Convert the map to a list of references.
	 * @param ownerId
	 * @param references
	 * @return
	 * @throws DatastoreException 
	 */
	public static List<DBOReference> createDBOReferences(Long ownerId, Map<String, Set<Reference>> map) throws DatastoreException{
		List<DBOReference> results = new ArrayList<DBOReference>();
		Iterator<String> groupIt = map.keySet().iterator();
		while(groupIt.hasNext()){
			String groupName = groupIt.next();
			Set<Reference> groupRef = map.get(groupName);
			for(Reference ref: groupRef){
				DBOReference dbo = new DBOReference();
				dbo.setGroupName(groupName);
				dbo.setOwner(ownerId);
				dbo.setTargetId(KeyFactory.stringToKey(ref.getTargetId()));
				dbo.setTargetRevision(ref.getTargetVersionNumber());
				results.add(dbo);
			}
		}
		return results;
	}

}
