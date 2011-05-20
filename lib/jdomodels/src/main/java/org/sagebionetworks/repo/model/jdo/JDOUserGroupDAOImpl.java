package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOUserGroupDAOImpl extends JDOBaseDAOImpl<UserGroup,JDOUserGroup> implements UserGroupDAOInitializingBean {
	
	UserGroup newDTO() {
		UserGroup ug = new UserGroup();
//		ug.setCreatableTypes(new HashSet<String>());
		return ug;
	}

	JDOUserGroup newJDO() {
		JDOUserGroup g = new JDOUserGroup();
		return g;
	}

	void copyToDto(JDOUserGroup jdo, UserGroup dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setName(jdo.getName());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setIndividual(jdo.getIsIndividual());
	}

	void copyFromDto(UserGroup dto, JDOUserGroup jdo)
		throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setIsIndividual(dto.isIndividual());
	}

	Class<JDOUserGroup> getJdoClass() {
		return JDOUserGroup.class;
	}
	
	/**
	 * @return the group matching the given name, and the given 'individual' property
	 */
	public UserGroup findGroup(String name, boolean isIndividual) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> groups = exec.execute(JDOUserGroup.class, 
				"pName==this.name && isIndividual==pIndividual",
				String.class.getName()+" pName, "+
				Boolean.class.getName()+" pIndividual",
				null,
				name,isIndividual);
		if (groups.size()>1) throw new DatastoreException("Expected 0-1 but found "+groups.size()+" for "+name);
		if (groups.size()==0) return null;
		JDOUserGroup jdo = groups.iterator().next();
		UserGroup dto = new UserGroup();
		copyToDto(jdo, dto);
		return dto;
	}
	
	/**
	 * @return the NON-system groups and the admin group (if applicable) for the list of group names
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupNames) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> groups = exec.execute(JDOUserGroup.class, 
				"pGroupNames.contains(this.name) && isIndividual==false",
				Collection.class.getName()+" pGroupNames, "+
				String.class.getName()+" pAdminGroupName, ",
				null,
				groupNames, AuthorizationConstants.ADMIN_GROUP_NAME);
		Map<String, UserGroup> ans = new HashMap<String, UserGroup>();
		for (JDOUserGroup jdo : groups) {
			UserGroup dto = new UserGroup();
			copyToDto(jdo, dto);
			ans.put(dto.getName(), dto);
		}
		return ans;
	}


	

	
	// initialization of UserGroups
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void afterPropertiesSet() throws Exception {
		// ensure public group is created
		{
			UserGroup pg = findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
			if (pg==null) {
				pg = new UserGroup();
				pg.setName(AuthorizationConstants.PUBLIC_GROUP_NAME);
				pg.setIndividual(false);
				create(pg);				
			}
		}
		{
			// ensure admin group is created
			UserGroup ag = findGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
			if (ag==null) {
				ag = new UserGroup();
				ag.setName(AuthorizationConstants.ADMIN_GROUP_NAME);
				ag.setIndividual(false);
				create(ag);				
			}
		}
	}
	
	
	public static String sqlCollection(Collection<? extends Object> c) {
		StringBuffer ans = new StringBuffer("(");
		boolean first = true;
		for (Object o : c) {
			if (first) first=false; else ans.append(",");
			ans.append(o.toString());
		}
		ans.append(")");
		return ans.toString();
	}

	
}

