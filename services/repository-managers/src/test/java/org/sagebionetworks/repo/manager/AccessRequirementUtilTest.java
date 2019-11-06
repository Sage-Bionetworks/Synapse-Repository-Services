package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;

public class AccessRequirementUtilTest {
	
	UserInfo userInfo;
	NodeDAO mockNodeDAO;
	AccessRequirementDAO mockAccessRequirementDAO;
	private static final String NODE_ID = "9876";
	private static RestrictableObjectDescriptor nodeRod;
	Node testEntityNode;
	List<Long> unmetARsDownload, unmetARsParticipate, unmetARsDownloadAndParticipate, unmetARsUpload;
	
	private List<ACCESS_TYPE> downloadParticipateAndSubmit;

	
	@Before
	public void before() throws Exception{
		nodeRod = new RestrictableObjectDescriptor();
		nodeRod.setId(NODE_ID);
		nodeRod.setType(RestrictableObjectType.ENTITY);
		String currentUserPrincipalId = "1234";
		userInfo = new UserInfo(false, currentUserPrincipalId);

		mockNodeDAO = mock(NodeDAO.class);
		
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		
		testEntityNode = new Node();
		testEntityNode.setId(NODE_ID);
		testEntityNode.setNodeType(EntityType.file);
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
		
		unmetARsUpload = new ArrayList<Long>();
		unmetARsUpload.add(3l);
		
		List<ACCESS_TYPE> downloadOnly = new ArrayList<ACCESS_TYPE>();
		downloadOnly.add(ACCESS_TYPE.DOWNLOAD);
		List<ACCESS_TYPE> participateOnly = new ArrayList<ACCESS_TYPE>();
		participateOnly.add(ACCESS_TYPE.PARTICIPATE);
		downloadParticipateAndSubmit = new ArrayList<ACCESS_TYPE>();
		downloadParticipateAndSubmit.addAll(downloadOnly);
		downloadParticipateAndSubmit.addAll(participateOnly);
		downloadParticipateAndSubmit.add(ACCESS_TYPE.SUBMIT);
		
		List<ACCESS_TYPE> uploadOnly = new ArrayList<ACCESS_TYPE>();
		uploadOnly.add(ACCESS_TYPE.UPLOAD);

		
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(any(List.class), any(RestrictableObjectType.class), any(Collection.class), eq(downloadOnly))).thenReturn(unmetARsDownload);
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(any(List.class), any(RestrictableObjectType.class), any(Collection.class), eq(participateOnly))).thenReturn(unmetARsParticipate);
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(any(List.class), any(RestrictableObjectType.class), any(Collection.class), eq(downloadParticipateAndSubmit))).thenReturn(unmetARsDownloadAndParticipate);
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(any(List.class), any(RestrictableObjectType.class), any(Collection.class), eq(uploadOnly))).thenReturn(unmetARsUpload);
	}
	
	@Test
	public void testOwnerEntityRequest() throws Exception {
		//current user requesting is the owner of the entity
		List<Long> unmetARs = AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(userInfo, NODE_ID, new ArrayList<String>(), mockNodeDAO, mockAccessRequirementDAO);
		assertTrue(unmetARs.size() == 0);
	}
	
	@Test
	public void testEntityRequest() throws Exception {
		//current user did not create the target node, should return unmet download ARs 
		testEntityNode.setCreatedByPrincipalId(42l);
		List<Long> unmetARs = AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(userInfo, NODE_ID, new ArrayList<String>(), mockNodeDAO, mockAccessRequirementDAO);
		assertEquals(unmetARsDownload, unmetARs);
	}

}
