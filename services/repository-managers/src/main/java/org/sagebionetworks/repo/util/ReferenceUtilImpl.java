package org.sagebionetworks.repo.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferenceUtilImpl implements ReferenceUtil {
	
	@Autowired
	NodeDAO nodeDao;

	/**
	 * When a current version is not provided we use the current.
	 * @param revisionNumber
	 * @param references
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void replaceNullVersionNumbersWithCurrent(Map<String, Set<Reference>> references) throws DatastoreException {
		if(references != null){
			// Find any revision that is missing its rev number;
			Iterator<Set<Reference>> it = references.values().iterator();
			while(it.hasNext()){
				Set<Reference> set = it.next();
				replaceNullVersionNumbersWithCurrent(set);
			}			
		}
	}

	@Override
	public void replaceNullVersionNumbersWithCurrent(Set<Reference> references) throws DatastoreException {
		if(references != null){
			for(Reference ref: references){
				if(ref.getTargetVersionNumber() == null){
					// Look up the current version number for this entity.
					try{
						Long currentRev = nodeDao.getCurrentRevisionNumber(ref.getTargetId());
						ref.setTargetVersionNumber(currentRev);
					}catch(NotFoundException e){
						// This node does not exist so set it to the default version.
						ref.setTargetVersionNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
					}
				}
			}			
		}
	}

}
