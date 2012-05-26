package org.sagebionetworks.repo.manager.backup.migration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeOwnerMigratorImpl implements NodeOwnerMigrator {
	private static Log log = LogFactory.getLog(NodeOwnerMigratorImpl.class);

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserManager userManager;
	
	private Long defaultPrincipalId;
	
	public NodeOwnerMigratorImpl() {}
	
	private Map<String,Long> principalCache = Collections.synchronizedMap(new HashMap<String,Long>());
	
	public NodeOwnerMigratorImpl(UserGroupDAO ugDAO, UserManager um) {
		userGroupDAO = ugDAO;
		userManager = um;
	}

	
	/*
	 * Need to have a 'default' owner so we can perform migration even if a user name
	 * is not found in the database.  
	 * 
	 * Our approach is to look through all the users until we find an administrator
	 * and use that user as the default.
	 * 
	 */
	private void chooseDefaultUserPrincipal() {
		try {
			Collection<UserGroup> principals = userGroupDAO.getAll(true/*isIndividual*/);
			for (UserGroup p : principals) {
				UserInfo userInfo = null;
				try {
					userInfo = userManager.getUserInfo(p.getName());
				} catch (NotFoundException nfe) {
					continue;
				}
				if (userInfo.isAdmin()) {
					defaultPrincipalId = Long.parseLong(p.getId());
					break;
				}
			}
			if (defaultPrincipalId==null) throw new IllegalStateException("No admin's found among the "+principals.size()+" known users.");
		} catch (DatastoreException e) {
			throw new RuntimeException(e);			
		}
	}
	
	@Override
	public EntityType migrateOneStep(NodeRevisionBackup toMigrate,
			EntityType type) {
		String modifierUserName = toMigrate.getModifiedBy();
		if (toMigrate.getModifiedByPrincipalId() == null) {
			// then we have to set it based on the modifiedBy user name
			toMigrate.setModifiedByPrincipalId(getUserPrincipal(modifierUserName));
		}
		return type;
	}
	
	@Override
	public Long getUserPrincipal(String userName) {
		if (defaultPrincipalId==null) chooseDefaultUserPrincipal();
		Long principalId = defaultPrincipalId;
		if (userName == null) {
			// use the default principal ID, set above
		} else {
			Long cachedValue = principalCache.get(userName);
			if (cachedValue!=null) {
				principalId = cachedValue;
			} else {
				Map<String, UserGroup> userGroupMap = new HashMap<String, UserGroup>();
				try {
					userGroupMap = userGroupDAO.getGroupsByNames(Arrays
							.asList(new String[] { userName }));
				} catch (DatastoreException e) {
					// log the error and proceed using the default
					log.warn(e);
				}
				UserGroup ug = userGroupMap.get(userName);
				if (ug == null) {
					// use the default principal ID, set above
				} else {
					principalId = Long.parseLong(ug.getId());
				}
				principalCache.put(userName, principalId);
			}
		}
		return principalId;
	}

}
