package org.sagebionetworks.repo.manager.dynamo;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.nodetree.IncompletePathException;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeUpdateDao;
import org.sagebionetworks.dynamo.dao.nodetree.ObsoleteChangeException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class NodeTreeUpdateManagerImplTest {

	private NodeTreeUpdateManager man;
	private NodeTreeUpdateDao nodeTreeDaoMock;
	private NodeDAO nodeDaoMock;

	private String cSuccess;
	private String pSuccess;
	private Date tSuccess;

	private String cRootSuccess;
	private Date tRootSuccess;

	private String cFailure;
	private String pFailure;
	private Date tFailure;

	private String cIncompletePath;
	private String pIncompletePath;
	private Date tIncompletePath;

	private String cObsolete;
	private String pObsolete;
	private Date tObsolete;

	private String cNotExistInRds;

	private String root;

	private void initIds() {
		Date date = new Date(System.currentTimeMillis() - 1000);
		this.cSuccess = KeyFactory.keyToString(1L);
		this.pSuccess = KeyFactory.keyToString(2L);
		this.tSuccess = date;
		this.cRootSuccess = KeyFactory.keyToString(21L);
		this.tRootSuccess = date;
		this.cFailure = KeyFactory.keyToString(3L);
		this.pFailure = KeyFactory.keyToString(4L);
		this.tFailure = date;
		this.cIncompletePath = KeyFactory.keyToString(5L);
		this.pIncompletePath = KeyFactory.keyToString(6L);
		this.tIncompletePath = date;
		this.cObsolete = KeyFactory.keyToString(7L);
		this.pObsolete = KeyFactory.keyToString(8L);
		this.tObsolete = date;
		this.cNotExistInRds = KeyFactory.keyToString(9L);
		this.root = KeyFactory.keyToString(11L);
	}

	private void mockNodeTreeDao() {

		this.nodeTreeDaoMock = mock(NodeTreeUpdateDao.class);

		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.pSuccess).toString(),
				this.tSuccess)).thenReturn(true);
		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess)).thenReturn(true);
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.pSuccess).toString(),
				this.tSuccess)).thenReturn(true);
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess)).thenReturn(true);
		when(this.nodeTreeDaoMock.delete(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess)).thenReturn(true);

		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				this.tRootSuccess)).thenReturn(true);
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				this.tRootSuccess)).thenReturn(true);

		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cFailure).toString(),
				KeyFactory.stringToKey(this.pFailure).toString(),
				this.tFailure)).thenReturn(false);
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cFailure).toString(),
				KeyFactory.stringToKey(this.pFailure).toString(),
				this.tFailure)).thenReturn(false);
		when(this.nodeTreeDaoMock.delete(
				KeyFactory.stringToKey(this.cFailure).toString(),
				this.tFailure)).thenReturn(false);

		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cIncompletePath).toString(),
				KeyFactory.stringToKey(this.pIncompletePath).toString(),
				this.tIncompletePath)).thenThrow(new IncompletePathException(""));
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cIncompletePath).toString(),
				KeyFactory.stringToKey(this.pIncompletePath).toString(),
				this.tIncompletePath)).thenThrow(new IncompletePathException(""));

		when(this.nodeTreeDaoMock.create(
				KeyFactory.stringToKey(this.cObsolete).toString(),
				KeyFactory.stringToKey(this.pObsolete).toString(),
				this.tObsolete)).thenThrow(new ObsoleteChangeException(""));
		when(this.nodeTreeDaoMock.update(
				KeyFactory.stringToKey(this.cObsolete).toString(),
				KeyFactory.stringToKey(this.pObsolete).toString(),
				this.tObsolete)).thenThrow(new ObsoleteChangeException(""));
		when(this.nodeTreeDaoMock.delete(
				KeyFactory.stringToKey(this.cObsolete).toString(),
				this.tObsolete)).thenThrow(new ObsoleteChangeException(""));
	}

	private void mockNodeDao() throws DatastoreException, NotFoundException {

		this.nodeDaoMock = mock(NodeDAO.class);

		List<EntityHeader> path = new ArrayList<EntityHeader>(3);
		EntityHeader eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.root);
		path.add(eh);
		eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.pIncompletePath);
		path.add(eh);
		eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.cIncompletePath);
		path.add(eh);
		when(this.nodeDaoMock.getEntityPath(this.cIncompletePath)).thenReturn(path);

		path = new ArrayList<EntityHeader>(2);
		eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.root);
		path.add(eh);
		eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.pObsolete);
		path.add(eh);
		eh = mock(EntityHeader.class);
		when(eh.getId()).thenReturn(this.cObsolete);
		path.add(eh);
		when(this.nodeDaoMock.getEntityPath(this.cObsolete)).thenReturn(path);
		when(this.nodeDaoMock.getNode(this.cObsolete)).thenThrow(new NotFoundException());
		when(this.nodeDaoMock.getParentId(this.cSuccess)).thenReturn(this.pSuccess);
		when(this.nodeDaoMock.getParentId(this.cRootSuccess)).thenReturn(null);
		when(this.nodeDaoMock.getParentId(this.cFailure)).thenReturn(this.pFailure);
		when(this.nodeDaoMock.getParentId(this.cIncompletePath)).thenReturn(this.pIncompletePath);
		when(this.nodeDaoMock.getParentId(this.cObsolete)).thenReturn(this.pObsolete);
		when(this.nodeDaoMock.getParentId(this.cNotExistInRds)).thenThrow(new NotFoundException());
	}
	
	@Before
	public void before() throws Exception {
		this.initIds();
		this.mockNodeTreeDao();
		this.mockNodeDao();

		this.man = new NodeTreeUpdateManagerImpl(this.nodeTreeDaoMock, this.nodeDaoMock);
		NodeTreeUpdateManager man = this.man;
		if(AopUtils.isAopProxy(man) && man instanceof Advised) {
			man = (NodeTreeUpdateManagerImpl)((Advised)man).getTargetSource().getTarget();
		}
		ReflectionTestUtils.setField(man, "nodeTreeUpdateDao", this.nodeTreeDaoMock);
		ReflectionTestUtils.setField(man, "nodeDao", this.nodeDaoMock);
	}

	@Test
	public void testCreateSuccess() {
		this.man.create(this.cSuccess, this.pSuccess, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).create(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.pSuccess).toString(),
				this.tSuccess);
	}

	@Test(expected=RuntimeException.class)
	public void testCreateFailure() {
		this.man.create(this.cFailure, this.pFailure, this.tFailure);
		verify(this.nodeTreeDaoMock, times(2)).create(
				KeyFactory.stringToKey(this.cFailure).toString(),
				KeyFactory.stringToKey(this.pFailure).toString(),
				this.tFailure);
	}

	@Test
	public void testCreateIncompletePathException() {
		this.man.create(this.cIncompletePath, this.pIncompletePath,
				this.tIncompletePath);
		verify(this.nodeTreeDaoMock, times(1)).create(
				KeyFactory.stringToKey(this.cIncompletePath).toString(),
				KeyFactory.stringToKey(this.pIncompletePath).toString(),
				this.tIncompletePath);
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.root).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.pIncompletePath).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(2)).create(
				eq(KeyFactory.stringToKey(this.cIncompletePath).toString()),
				eq(KeyFactory.stringToKey(this.pIncompletePath).toString()),
				any(Date.class));
		// Should call in total 4 times -- the 1st time to generate the incomplete path exception
		// and then 3 time to rebuild the path
		verify(this.nodeTreeDaoMock, times(4)).create(any(String.class), any(String.class),
				any(Date.class));
	}

	@Test
	public void testCreateObsoleteChangeException() {
		this.man.create(this.cObsolete, this.pObsolete,
				this.tObsolete);
		verify(this.nodeTreeDaoMock, times(1)).create(
				KeyFactory.stringToKey(this.cObsolete).toString(),
				KeyFactory.stringToKey(this.pObsolete).toString(),
				this.tObsolete);
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.root).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.pObsolete).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(2)).create(
				eq(KeyFactory.stringToKey(this.cObsolete).toString()),
				eq(KeyFactory.stringToKey(this.pObsolete).toString()),
				any(Date.class));
	}

	@Test
	public void testUpdateSuccess() {
		this.man.update(this.cSuccess, this.pSuccess, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).update(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.pSuccess).toString(),
				this.tSuccess);
	}

	@Test(expected=RuntimeException.class)
	public void testUpdateFailure() {
		this.man.update(this.cFailure, this.pFailure, this.tFailure);
		verify(this.nodeTreeDaoMock, times(2)).update(
				KeyFactory.stringToKey(this.cFailure).toString(),
				KeyFactory.stringToKey(this.pFailure).toString(),
				this.tFailure);
	}

	@Test
	public void testUpdateIncompletePathException() {
		this.man.update(this.cIncompletePath, this.pIncompletePath,
				this.tIncompletePath);
		verify(this.nodeTreeDaoMock, times(1)).update(
				KeyFactory.stringToKey(this.cIncompletePath).toString(),
				KeyFactory.stringToKey(this.pIncompletePath).toString(),
				this.tIncompletePath);
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.root).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.pIncompletePath).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.cIncompletePath).toString()),
				eq(KeyFactory.stringToKey(this.pIncompletePath).toString()),
				any(Date.class));
		// Should call in total 3 times to rebuild the path
		verify(this.nodeTreeDaoMock, times(3)).create(any(String.class), any(String.class),
				any(Date.class));
	}

	@Test
	public void testUpdateObsoleteChangeException() {
		this.man.update(this.cObsolete, this.pObsolete,
				this.tObsolete);
		verify(this.nodeTreeDaoMock, times(1)).update(
				KeyFactory.stringToKey(this.cObsolete).toString(),
				KeyFactory.stringToKey(this.pObsolete).toString(),
				this.tObsolete);
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.root).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.pObsolete).toString()),
				eq(KeyFactory.stringToKey(this.root).toString()),
				any(Date.class));
		verify(this.nodeTreeDaoMock, times(1)).create(
				eq(KeyFactory.stringToKey(this.cObsolete).toString()),
				eq(KeyFactory.stringToKey(this.pObsolete).toString()),
				any(Date.class));
	}

	@Test
	public void testDeleteSuccess() {
		this.man.delete(this.cSuccess, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).delete(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess);
	}

	@Test(expected=RuntimeException.class)
	public void testDeleteFailure() {
		this.man.delete(this.cFailure, this.tFailure);
		verify(this.nodeTreeDaoMock, times(2)).delete(
				KeyFactory.stringToKey(this.cFailure).toString(),
				this.tFailure);
	}

	@Test
	public void testDeleteObsoleteChangeException() {
		this.man.delete(this.cObsolete, this.tObsolete);
		verify(this.nodeTreeDaoMock, times(2)).delete(
				eq(KeyFactory.stringToKey(this.cObsolete).toString()),
				any(Date.class));
	}

	@Test
	public void testCreateRoot() {
		this.man.create(this.cRootSuccess, null, this.tRootSuccess);
		verify(this.nodeTreeDaoMock, times(1)).create(
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				this.tRootSuccess);
	}

	@Test
	public void testUpdateRoot() {
		this.man.update(this.cRootSuccess, null, this.tRootSuccess);
		verify(this.nodeTreeDaoMock, times(1)).update(
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				KeyFactory.stringToKey(this.cRootSuccess).toString(),
				this.tRootSuccess);
	}

	@Test
	public void testNodeNotExitInRds() {
		this.man.create(this.cNotExistInRds, this.pSuccess, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).delete(
				eq(KeyFactory.stringToKey(this.cNotExistInRds).toString()),
				any(Date.class));
		this.man.update(this.cNotExistInRds, this.pSuccess, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(2)).delete(
				eq(KeyFactory.stringToKey(this.cNotExistInRds).toString()),
				any(Date.class));
	}
}
