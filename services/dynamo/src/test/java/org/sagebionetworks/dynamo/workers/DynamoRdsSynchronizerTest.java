package org.sagebionetworks.dynamo.workers;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.dynamo.manager.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.QueryResults;

public class DynamoRdsSynchronizerTest {

	@Test
	public void testTheSame() {

		NodeParentRelation childParent = new NodeParentRelation();
		childParent.setId("syn1");
		childParent.setParentId("syn11");

		List<NodeParentRelation> results = new ArrayList<NodeParentRelation>();
		results.add(childParent);
		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(100L);

		NodeDAO nodeDao = mock(NodeDAO.class);
		when(nodeDao.getParentRelations(anyLong(), eq(1L))).thenReturn(queryResults);

		NodeTreeDao nodeTreeDao = mock(NodeTreeDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("11");

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);
		
		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		sync.triggerFired();

		verify(nodeTreeUpdateManager, times(0)).create(anyString(), anyString(), any(Date.class));
		verify(nodeTreeUpdateManager, times(0)).update(anyString(), anyString(), any(Date.class));
	}

	@Test
	public void testChildNotExist() {

		NodeParentRelation childParent = new NodeParentRelation();
		childParent.setId("syn1");
		childParent.setParentId("syn11");

		List<NodeParentRelation> results = new ArrayList<NodeParentRelation>();
		results.add(childParent);
		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(100L);

		NodeDAO nodeDao = mock(NodeDAO.class);
		when(nodeDao.getParentRelations(anyLong(), eq(1L))).thenReturn(queryResults);

		NodeTreeDao nodeTreeDao = mock(NodeTreeDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn(null);

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);
		
		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		sync.triggerFired();

		verify(nodeTreeUpdateManager, times(1)).create(eq("syn1"), eq("syn11"), any(Date.class));
		verify(nodeTreeUpdateManager, times(0)).update(anyString(), anyString(), any(Date.class));
	}

	@Test
	public void testDifferentParent() {

		NodeParentRelation childParent = new NodeParentRelation();
		childParent.setId("syn1");
		childParent.setParentId("syn11");

		List<NodeParentRelation> results = new ArrayList<NodeParentRelation>();
		results.add(childParent);
		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(100L);

		NodeDAO nodeDao = mock(NodeDAO.class);
		when(nodeDao.getParentRelations(anyLong(), eq(1L))).thenReturn(queryResults);

		NodeTreeDao nodeTreeDao = mock(NodeTreeDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("21");

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);
		
		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		sync.triggerFired();

		verify(nodeTreeUpdateManager, times(0)).create(anyString(), anyString(), any(Date.class));
		verify(nodeTreeUpdateManager, times(1)).update(eq("syn1"), eq("syn11"), any(Date.class));
	}

	@Test
	public void testRoot() {

		NodeParentRelation childParent = new NodeParentRelation();
		childParent.setId("syn1");
		childParent.setParentId(null);

		List<NodeParentRelation> results = new ArrayList<NodeParentRelation>();
		results.add(childParent);
		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(100L);

		NodeDAO nodeDao = mock(NodeDAO.class);
		when(nodeDao.getParentRelations(anyLong(), eq(1L))).thenReturn(queryResults);

		NodeTreeDao nodeTreeDao = mock(NodeTreeDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("11");
		when(nodeTreeDao.getRoot()).thenReturn("1");

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);
		
		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		sync.triggerFired();

		verify(nodeTreeDao, times(1)).getRoot();
	}
}
