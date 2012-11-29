package org.sagebionetworks.dynamo.manager;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.IncompletePathException;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.dynamo.dao.ObsoleteChangeException;
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
	private NodeTreeDao nodeTreeDaoMock;
	private NodeDAO nodeDaoMock;

	private String cSuccess;
	private String pSuccess;
	private Date tSuccess;

	private String cFailure;
	private String pFailure;
	private Date tFailure;

	private String cIncompletePath;
	private String pIncompletePath;
	private Date tIncompletePath;

	private String cObsolete;
	private String pObsolete;
	private Date tObsolete;

	private String root;

	private void initIds() {
		String prefix = "syn";
		Random random = new Random();
		int max = 1981928298;
		this.cSuccess = prefix + Integer.toString(random.nextInt(max));
		this.pSuccess = prefix + Integer.toString(random.nextInt(max));
		this.tSuccess = new Date();
		this.cFailure = prefix + Integer.toString(random.nextInt(max));
		this.pFailure = prefix + Integer.toString(random.nextInt(max));
		this.tFailure = new Date();
		this.cIncompletePath = prefix + Integer.toString(random.nextInt(max));
		this.pIncompletePath = prefix + Integer.toString(random.nextInt(max));
		this.tIncompletePath = new Date();
		this.cObsolete = prefix + Integer.toString(random.nextInt(max));
		this.pObsolete = prefix + Integer.toString(random.nextInt(max));
		this.tObsolete = new Date();
		this.root = prefix + Integer.toString(random.nextInt(max));
	}

	private void mockNodeTreeDao() {

		this.nodeTreeDaoMock = mock(NodeTreeDao.class);

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
		ReflectionTestUtils.setField(man, "nodeTreeDao", this.nodeTreeDaoMock);
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
		this.man.create(this.cSuccess, null, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).create(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess);
	}
	
	@Test
	public void testUpdateRoot() {
		this.man.update(this.cSuccess, null, this.tSuccess);
		verify(this.nodeTreeDaoMock, times(1)).update(
				KeyFactory.stringToKey(this.cSuccess).toString(),
				KeyFactory.stringToKey(this.cSuccess).toString(),
				this.tSuccess);
	}
}
