package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class AuthorizationManagerImplUnitTest {
	
	NodeInheritanceDAO mockNodeInheritanceDAO;	
	AccessControlListDAO mockAccessControlListDAO;	
	AccessRequirementDAO  mockAccessRequirementDAO;
	ActivityDAO mockActivityDAO;
	NodeQueryDao mockNodeQueryDao;	
	NodeDAO mockNodeDAO;
	UserManager mockUserManager;
	
	AuthorizationManager authorizationManager = null;
	UserInfo userInfo = null;
	UserInfo adminUser = null;
	
	@Before
	public void setUp() throws Exception {
		mockNodeInheritanceDAO = mock(NodeInheritanceDAO.class);	
		mockAccessControlListDAO = mock(AccessControlListDAO.class);	
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockNodeQueryDao = mock(NodeQueryDao.class);	
		mockNodeDAO = mock(NodeDAO.class);
		mockUserManager = mock(UserManager.class);
		
		authorizationManager = new AuthorizationManagerImpl(
				mockNodeInheritanceDAO, mockAccessControlListDAO,
				mockAccessRequirementDAO, mockActivityDAO, mockNodeQueryDao,
				mockNodeDAO, mockUserManager);
		
		userInfo = new UserInfo(false);
		UserGroup userInfoGroup = new UserGroup();
		userInfoGroup.setId("123");
		userInfo.setIndividualGroup(userInfoGroup);
		adminUser = new UserInfo(true);
		UserGroup adminInfoGroup = new UserGroup();
		adminInfoGroup.setId("456");
		adminUser.setIndividualGroup(adminInfoGroup);		
	}

	@Test
	public void testCanAccessActivityPagination() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int total = 2001;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUser.getIndividualGroup().getId());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		QueryResults<String> results1 = generateQueryResults(limit, total);
		QueryResults<String> results2 = generateQueryResults(total-limit, total);		
		QueryResults<String> results3 = generateQueryResults(total-(2*limit), total);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+limit)).thenReturn(results2);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+(2*limit))).thenReturn(results3);
		
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+limit);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+(2*limit));
		assertFalse(canAccess);
	}

	@Test
	public void testCanAccessActivityPaginationSmallResultSet() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUser.getIndividualGroup().getId());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		QueryResults<String> results1 = generateQueryResults(1, 1);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);		
		
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		assertFalse(canAccess);
	}

	private QueryResults<String> generateQueryResults(int numResults, int total) {
		QueryResults<String> results = new QueryResults<String>();
		List<String> resultList = new ArrayList<String>();		
		for(int i=0; i<numResults; i++) {
			resultList.add("nodeId");
		}
		results.setResults(resultList);
		results.setTotalNumberOfResults(total);
		return results;
	}
	
}
