package org.sagebionetworks.repo.model.bootstrap;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author jmhill
 *
 */
@Transactional(readOnly = false)
public class EntityBootstrapperImpl implements EntityBootstrapper {
	
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	@Autowired
	NodeInheritanceDAO nodeInheritanceDao;

	private List<EntityBootstrapData> bootstrapEntities;
	/**
	 * Map EntityBootstrapData using its path.
	 */
	private Map<String, EntityBootstrapData> pathMap;

	@Override
	public void afterPropertiesSet() throws Exception {
		// First make sure the nodeDao has been boostraped
		nodeDao.boostrapAllNodeTypes();
		pathMap = Collections.synchronizedMap(new HashMap<String, EntityBootstrapData>());
		// Map the default users to their ids
		 Map<DEFAULT_GROUPS, String> groupIdMap = buildGroupMap();
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
			toCreate.setCreatedBy(NodeConstants.BOOTSTRAP_USERNAME);
			toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
			toCreate.setModifiedBy(NodeConstants.BOOTSTRAP_USERNAME);
			toCreate.setModifiedOn(toCreate.getCreatedOn());
			toCreate.setVersionComment(NodeConstants.DEFAULT_VERSION_LABEL);
			String nodeId = nodeDao.createNew(toCreate);
			// Now create the ACL on the node
			AccessControlList acl = createAcl(nodeId, entityBoot.getAccessList(), groupIdMap);
			// Now set the ACL for this node.
			accessControlListDAO.create(acl);
			nodeInheritanceDao.addBeneficiary(nodeId, nodeId);
		}
	}
	
	/**
	 * Build up a map of group IDs for each group.
	 */
	public Map<DEFAULT_GROUPS, String> buildGroupMap() throws DatastoreException{
		 Map<DEFAULT_GROUPS, String> groupIdMap = new HashMap<DEFAULT_GROUPS, String>();
		 // Look up each group
		 DEFAULT_GROUPS[] array = DEFAULT_GROUPS.values();
		 for(DEFAULT_GROUPS group: array){
			 UserGroup ug = userGroupDAO.findGroup(group.name(), false);
			 groupIdMap.put(group, ug.getId());
		 }
		 return groupIdMap;
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
		if(path == null) throw new IllegalArgumentException("Bootstrap 'enityPath' cannot be null");
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
	public static AccessControlList createAcl(String nodeId, List<AccessBootstrapData> list, Map<DEFAULT_GROUPS, String> groupIdMap){
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy(NodeConstants.BOOTSTRAP_USERNAME);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceId(nodeId);
		acl.setModifiedBy(acl.getCreatedBy());
		acl.setModifiedOn(acl.getCreationDate());
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		for(AccessBootstrapData data: list){
			// For each group add the types requested.
			data.getGroup();
			ResourceAccess access = new ResourceAccess();
			set.add(access);
			Set<ACCESS_TYPE> typeSet = new HashSet<ACCESS_TYPE>();
			access.setAccessType(typeSet);
			access.setUserGroupId(groupIdMap.get(data.getGroup()));
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
