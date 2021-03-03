package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeDaoObjectHelper implements DaoObjectHelper<Node> {

	@Autowired
	private NodeDAO nodeDao;

	@Override
	public Node create(Consumer<Node> consumer) {
		// setup the default
		Node n = new Node();
		n.setName(UUID.randomUUID().toString());
		n.setCreatedByPrincipalId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		n.setETag(UUID.randomUUID().toString());
		n.setModifiedOn(new Date());
		n.setCreatedOn(new Date());
		n.setParentId(null);
		n.setNodeType(EntityType.project);

		// give the call a chance to override.
		consumer.accept(n);

		if (n.getModifiedByPrincipalId() == null) {
			n.setModifiedByPrincipalId(n.getCreatedByPrincipalId());
		}
		return nodeDao.createNewNode(n);
	}

}
