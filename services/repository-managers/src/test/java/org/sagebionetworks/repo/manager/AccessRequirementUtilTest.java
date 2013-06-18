package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;

public class AccessRequirementUtilTest {
	
	RestrictableObjectDescriptor subjectId;
	UserInfo userInfo;
	UserGroup userGroup;
	NodeDAO mockNodeDAO;
	AccessRequirementDAO mockAccessRequirementDAO;
	Node testEntityNode;
	List<Long> unmetARsDownload, unmetARsParticipate, unmetARsDownloadAndParticipate;
	
	@Before
	public void before() throws Exception{
		userInfo = new UserInfo(false);
		userGroup = new UserGroup();
		userInfo.setGroups(new ArrayList<UserGroup>());
		String currentUserPrincipalId = "1234";
		userGroup.setId(currentUserPrincipalId);
		userInfo.setIndividualGroup(userGroup);
		mockNodeDAO = mock(NodeDAO.class);
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		subjectId = new RestrictableObjectDescriptor();
		//default set subject to ENTITY and owned by the current user
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId(currentUserPrincipalId);
		
		testEntityNode = new Node();
		//by default, set node created by to current user
		testEntityNode.setCreatedByPrincipalId(Long.parseLong(currentUserPrincipalId));
		when(mockNodeDAO.getNode(anyString())).thenReturn(testEntityNode);
		unmetARsDownload = new ArrayList<Long>();
		unmetARsDownload.add(1l);
		unmetARsParticipate = new ArrayList<Long>();
		unmetARsParticipate.add(2l);
		
		unmetARsDownloadAndParticipate = new ArrayList<Long>();
		unmetARsDownloadAndParticipate.addAll(unmetARsDownload);
		unmetARsDownloadAndParticipate.addAll(unmetARsParticipate);
		
		List<ACCESS_TYPE> downloadOnly = new ArrayList<ACCESS_TYPE>();
		downloadOnly.add(ACCESS_TYPE.DOWNLOAD);
		List<ACCESS_TYPE> participateOnly = new ArrayList<ACCESS_TYPE>();
		participateOnly.add(ACCESS_TYPE.PARTICIPATE);
		List<ACCESS_TYPE> downloadAndParticipate = new ArrayList<ACCESS_TYPE>();
		downloadAndParticipate.addAll(downloadOnly);
		downloadAndParticipate.addAll(participateOnly);
		
		when(mockAccessRequirementDAO.unmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(Collection.class), eq(downloadOnly))).thenReturn(unmetARsDownload);
		when(mockAccessRequirementDAO.unmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(Collection.class), eq(participateOnly))).thenReturn(unmetARsParticipate);
		when(mockAccessRequirementDAO.unmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(Collection.class), eq(downloadAndParticipate))).thenReturn(unmetARsDownloadAndParticipate);
	}
	
	@Test
	public void testOwnerEntityRequest() throws Exception {
		//current user requesting is the owner of the entity
		List<Long> unmetARs = AccessRequirementUtil.unmetAccessRequirementIds(userInfo, subjectId, mockNodeDAO, mockAccessRequirementDAO);
		assertTrue(unmetARs.size() == 0);
	}
	
	@Test
	public void testEntityRequest() throws Exception {
		//current user did not create the target node, should return unmet download ARs 
		testEntityNode.setCreatedByPrincipalId(42l);
		List<Long> unmetARs = AccessRequirementUtil.unmetAccessRequirementIds(userInfo, subjectId, mockNodeDAO, mockAccessRequirementDAO);
		assertEquals(unmetARsDownload, unmetARs);
	}
	
	@Test
	public void testEvaluationRequest() throws Exception {
		//verify both download and participate ARs are returned
		subjectId.setType(RestrictableObjectType.EVALUATION);
		List<Long> unmetARs = AccessRequirementUtil.unmetAccessRequirementIds(userInfo, subjectId, mockNodeDAO, mockAccessRequirementDAO);
		assertEquals(unmetARsDownloadAndParticipate, unmetARs);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testInvalidSubjectTypeRequest() throws Exception {
		//verify both download and participate ARs are returned
		subjectId.setType(null);
		AccessRequirementUtil.unmetAccessRequirementIds(userInfo, subjectId, mockNodeDAO, mockAccessRequirementDAO);
	}
}
