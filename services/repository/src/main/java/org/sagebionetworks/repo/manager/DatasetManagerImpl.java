package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DatasetManagerImpl implements DatasetManager {
	
	@Autowired
	NodeManager nodeManager;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Dataset createDataset(String userId, Dataset newEntity) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		if(newEntity == null) throw new IllegalArgumentException("Dataset cannot be null");
		// First create a new dataset
		Node node = NodeTranslationUtils.createFromBase(newEntity);
		// We are ready to create this node
		String nodeId = nodeManager.createNewNode(node, userId);
		// Now get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userId, nodeId);
		// Now add all of the annotations from the dataset
		
		// Create the node
		return null;
	}

}
