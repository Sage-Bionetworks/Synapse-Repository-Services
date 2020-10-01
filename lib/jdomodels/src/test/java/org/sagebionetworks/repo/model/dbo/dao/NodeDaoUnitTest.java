package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class NodeDaoUnitTest {
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;

	@Mock
	private NamedParameterJdbcTemplate mockNamedParameterJdbcTemplate;

	@Mock
	private IdGenerator mockIdGenerator;
	
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	@Mock
	private DBOBasicDao mockDboBasicDao;
	
	@InjectMocks
	private NodeDAOImpl nodeDao;
	
	@Captor
	private ArgumentCaptor<MessageToSend> messageCaptor;
	
	private Node node;
	
	@BeforeEach
	public void before() {
		node = new Node();
		node.setCreatedByPrincipalId(123L);
		node.setCreatedOn(new Date());
		node.setId("syn456");
		node.setETag("someEtag");
		node.setModifiedByPrincipalId(123L);
		node.setModifiedOn(new Date());
		node.setName("name");
		node.setNodeType(EntityType.file);
		
	}
	
	@Test
	public void testGetNamesFromPath(){
		String path = "/root";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(2, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
	}
	
	@Test
	public void testGetNamesFromPathMissingPrefix(){
		String path = "root";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(2, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
	}
	
	@Test
	public void testGetNamesFromPathLonger(){
		String path = "/root/some other name/Lots ";
		List<String> names = NodeDAOImpl.getNamesFromPath(path);
		assertNotNull(names);
		assertEquals(4, names.size());
		assertEquals("/", names.get(0));
		assertEquals("root", names.get(1));
		assertEquals("some other name", names.get(2));
		assertEquals("Lots", names.get(3));
	}
	
	@Test
	public void testCreatePathQueryRoot(){
		String path = "/root";
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = NodeDAOImpl.createPathQuery(path, params);
		assertNotNull(sql);
		System.out.println(sql);
		String param = "nam1";
		assertEquals("root", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
	}
	
	@Test
	public void testCreatePathQueryLonger(){
		String path = "/root/parent/child";
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = NodeDAOImpl.createPathQuery(path, params);
		assertNotNull(sql);
		assertEquals(3, params.size());
		System.out.println(sql);
		String param = "nam1";
		assertEquals("root", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
		
		param = "nam2";
		assertEquals("parent", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
		
		param = "nam3";
		assertEquals("child", params.get(param));
		assertTrue(sql.indexOf(param) > 0);
	}
	
	@Test
	public void testCreate() {
		// call under test
		nodeDao.createNewNode(node);
		// validate the message was sent.
		verify(mockTransactionalMessenger).sendMessageAfterCommit(this.messageCaptor.capture());
		MessageToSend sent = messageCaptor.getValue();
		assertNotNull(sent);
		assertEquals("syn0", sent.getObjectId());
		assertEquals(ObjectType.ENTITY, sent.getObjectType());
		assertEquals(node.getCreatedByPrincipalId(), sent.getUserId());
		assertEquals(ChangeType.CREATE, sent.getChangeType());
	}
	
	@Test
	public void testTouch() {
		Long userId = 123L;
		String nodeId = "syn456";
		// call under test
		nodeDao.touch(userId, nodeId);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(this.messageCaptor.capture());
		MessageToSend sent = messageCaptor.getValue();
		assertNotNull(sent);
		assertEquals(nodeId, sent.getObjectId());
		assertEquals(ObjectType.ENTITY, sent.getObjectType());
		assertEquals(userId, sent.getUserId());
		assertEquals(ChangeType.UPDATE, sent.getChangeType());
	}
	
	@Test
	public void testTouchWithChangeType() {
		Long userId = 123L;
		String nodeId = "syn456";
		ChangeType changeType = ChangeType.DELETE;
		// call under test
		nodeDao.touch(userId, nodeId, changeType);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(this.messageCaptor.capture());
		MessageToSend sent = messageCaptor.getValue();
		assertNotNull(sent);
		assertEquals(nodeId, sent.getObjectId());
		assertEquals(ObjectType.ENTITY, sent.getObjectType());
		assertEquals(userId, sent.getUserId());
		assertEquals(changeType, sent.getChangeType());
	}

	@Test
	public void testDelete() {
		String nodeId = "syn456";
		// call under test
		nodeDao.delete(nodeId);
		verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(nodeId, ObjectType.ENTITY);
	}
	
	@Test
	public void testDeleteTreeWithEmtpySubtree() {
		String nodeId = "syn456";
		Long longId = 456L;
		
		int limit = 2;
		
		List<Long> subTree = Collections.emptyList();
		
		when(mockJdbcTemplate.queryForList(anyString(), eq(Long.class), eq(longId), eq(limit + 1)))
			.thenReturn(subTree);
			
		// call under test
		boolean result = nodeDao.deleteTree(nodeId, limit);
		
		assertTrue(result);
		verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(nodeId, ObjectType.ENTITY);
	}
	
	@Test
	public void testDeleteTreeWithSubtree() {
		String nodeId = "syn456";
		Long longId = 456L;
		
		int limit = 2;
		
		List<Long> subTree = Arrays.asList(123L, 678L);
		
		when(mockJdbcTemplate.queryForList(anyString(), eq(Long.class), eq(longId), eq(limit + 1)))
			.thenReturn(subTree);
			
		// call under test
		boolean result = nodeDao.deleteTree(nodeId, limit);
		
		// The subtree size is lesser or equal to the limit, the node is deleted
		assertTrue(result);
		verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(nodeId, ObjectType.ENTITY);
	}
	
	@Test
	public void testDeleteTreeWithSubtreeGreaterThanLimit() {
		String nodeId = "syn456";
		Long longId = 456L;
		
		int limit = 2;
		
		List<Long> subTree = Arrays.asList(123L, 678L, 768L);
		
		when(mockJdbcTemplate.queryForList(anyString(), eq(Long.class), eq(longId), eq(limit + 1)))
			.thenReturn(subTree);
			
		// call under test
		boolean result = nodeDao.deleteTree(nodeId, limit);
		
		// The subtree size is greater than the limit, the node is not deleted
		assertFalse(result);
		verifyZeroInteractions(mockTransactionalMessenger);
	}
	
	@Test
	public void testUpdateAnnotations_nullNodeId(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			nodeDao.updateAnnotations(null, new Annotations(), "any columname works");
		});
	}

	@Test
	public void testUpdateAnnotations_nullAnnotations(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			nodeDao.updateAnnotations("syn123", null, "any columname works");
		});
	}
	
	@Test
	public void testUpdateRevisionFileHandle() {
		String nodeId = "123";
		Long versionNumber = 1L;
		String newFileHandleId = "1234";
		
		int updatedRows = 1;
		
		when(mockJdbcTemplate.update(anyString(), anyLong(), anyLong(), anyLong())).thenReturn(updatedRows);
		
		// Call under test
		boolean result = nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
	
		assertTrue(result);
		
		verify(mockJdbcTemplate).update("UPDATE JDOREVISION SET FILE_HANDLE_ID = ? WHERE OWNER_NODE_ID = ? AND NUMBER = ?", Long.valueOf(newFileHandleId), KeyFactory.stringToKey(nodeId), versionNumber);
	}
	
	@Test
	public void testUpdateRevisionFileHandleWithNoId() {
		String nodeId = null;
		Long versionNumber = 1L;
		String newFileHandleId = "1234";
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		}).getMessage();
		
		assertEquals("The nodeId is required.", errorMessage);
	}
	
	@Test
	public void testUpdateRevisionFileHandleWithNoRevision() {
		String nodeId = "123";
		Long versionNumber = null;
		String newFileHandleId = "1234";
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		}).getMessage();
		
		assertEquals("The versionNumber is required.", errorMessage);
	}
	
	@Test
	public void testUpdateRevisionFileHandleWithNoFileHandle() {
		String nodeId = "123";
		Long versionNumber = 1L;
		String newFileHandleId = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		}).getMessage();
		
		assertEquals("The fileHandleId is required.", errorMessage);
	}
}
