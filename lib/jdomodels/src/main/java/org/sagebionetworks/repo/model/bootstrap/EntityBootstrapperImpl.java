package org.sagebionetworks.repo.model.bootstrap;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author jmhill
 *
 */
public class EntityBootstrapperImpl implements EntityBootstrapper {
	
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	NodeInheritanceDAO nodeInheritanceDao;

	private List<EntityBootstrapData> bootstrapEntities;
	/**
	 * Map EntityBootstrapData using its path.
	 */
	private Map<String, EntityBootstrapData> pathMap;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void bootstrapAll() throws Exception {
		// Make sure users and profiles have been boostraped
		userGroupDAO.bootstrapUsers();
		userProfileDAO.bootstrapProfiles();
		// First make sure the nodeDao has been boostraped
		nodeDao.boostrapAllNodeTypes();
		pathMap = Collections.synchronizedMap(new HashMap<String, EntityBootstrapData>());
		UserGroup bootstrapPrincipal = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		// Map the default users to their ids
		// Now create a node for each type in the list
		for(EntityBootstrapData entityBoot: bootstrapEntities){
			// Only add this node if it does not already exists
			if(entityBoot.getEntityPath() == null) throw new IllegalArgumentException("Bootstrap 'enityPath' cannot be null");
			if(entityBoot.getDefaultChildAclScheme() == null) throw new IllegalArgumentException("Boostrap 'defaultChildAclScheme' cannot be null");
			// Add this to the map
			pathMap.put(entityBoot.getEntityPath(), entityBoot);
			// The very first time we try to run a query it might 
			String id = nodeDao.getNodeIdForPath(entityBoot.getEntityPath());
			// Does this already exist?
			if(id != null) continue;
			// Create the entity
			Node toCreate = new Node();
			// Get the name and parent from the path
			String[] parentAndName = splitParentPathAndName(entityBoot.getEntityPath());
			// Look up the parent if it exists
			String parentPath = parentAndName[0];
			String parentId = null;
			if(parentPath != null){
				parentId = nodeDao.getNodeIdForPath(parentPath);
				if(parentId == null) throw new IllegalArgumentException("Cannot find a parent with a path: "+parentPath);
			}
			toCreate.setName(parentAndName[1]);
			toCreate.setParentId(parentId);
			toCreate.setDescription(entityBoot.getEntityDescription());
			if(entityBoot.getEntityType() == null) throw new IllegalArgumentException("Bootstrap 'entityType' cannot be null");
			toCreate.setNodeType(entityBoot.getEntityType().name());
			toCreate.setCreatedByPrincipalId(Long.parseLong(bootstrapPrincipal.getId()));
			toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
			toCreate.setModifiedByPrincipalId(Long.parseLong(bootstrapPrincipal.getId()));
			toCreate.setModifiedOn(toCreate.getCreatedOn());
			toCreate.setVersionComment(NodeConstants.DEFAULT_VERSION_LABEL);
			toCreate.setId(""+entityBoot.getEntityId());
			String nodeId = nodeDao.createNew(toCreate);
			
			for (AccessBootstrapData abd: entityBoot.getAccessList()) {
				Long groupId = Long.parseLong(userGroupDAO.findGroup(abd.getGroup().name(), false).getId());
				abd.setGroupId(groupId);
			}

			// Now create the ACL on the node
			AccessControlList acl = createAcl(nodeId, bootstrapPrincipal.getId(), entityBoot.getAccessList());
			// Now set the ACL for this node.
			aclDAO.create(acl);
			nodeInheritanceDao.addBeneficiary(nodeId, nodeId);

			// Verify the bootstrap entity has indeed been created
			id = nodeDao.getNodeIdForPath(entityBoot.getEntityPath());
			if (id == null) {
				throw new DatastoreException("Bootstrapping failed for entity path " + entityBoot.getEntityPath() );
			}
		}
	}

	@Override
	public List<EntityBootstrapData> getBootstrapEntities() {
		return bootstrapEntities;
	}


	public void setBootstrapEntities(List<EntityBootstrapData> bootstrapEntities) {
		this.bootstrapEntities = bootstrapEntities;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public static String[] splitParentPathAndName(String path){
		if(path == null) throw new IllegalArgumentException("Bootstrap 'entityPath' cannot be null");
		path = path.trim();
		int index = path.lastIndexOf(NodeConstants.PATH_PREFIX);
		String[] results = new String[2];
		if(index > 0){
			results[0] = path.substring(0, index);
		}
		results[1] = path.substring(index+1, path.length());
		return results;
	}
	
	/**
	 * Build up an ACL from List<AccessBootstrapData> list.
	 * @param nodeId
	 * @param list
	 * @param groupIdMap
	 * @return
	 */
	public static AccessControlList createAcl(String nodeId, String creatorPrincipalId, List<AccessBootstrapData> list) throws DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setId(nodeId);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		for(AccessBootstrapData data: list){
			// For each group add the types requested.
			ResourceAccess access = new ResourceAccess();
			set.add(access);
			Set<ACCESS_TYPE> typeSet = new HashSet<ACCESS_TYPE>();
			access.setAccessType(typeSet);
			access.setPrincipalId(data.getGroupId());
			// Add each type to the set
			List<ACCESS_TYPE> types = data.getAccessTypeList();
			for(ACCESS_TYPE type: types){
				typeSet.add(type);
			}
		}
		return acl;
	}

	@Override
	public ACL_SCHEME getChildAclSchemeForPath(String path) {
		// First get the data for this path
		EntityBootstrapData data =  pathMap.get(path);
		if(data == null) throw new IllegalArgumentException("Cannot find the EntityBootstrapData for path: "+path);
		return data.getDefaultChildAclScheme();
	}

}
