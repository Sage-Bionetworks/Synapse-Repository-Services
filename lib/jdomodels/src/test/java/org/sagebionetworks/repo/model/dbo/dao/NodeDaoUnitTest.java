package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
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
	ArgumentCaptor<MessageToSend> messageCaptor;
	
	Node node;
	
	@Before
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

}
