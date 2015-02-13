package org.sagebionetworks.dynamo.workers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.QueryResults;
import org.springframework.test.util.ReflectionTestUtils;

public class DynamoRdsSynchronizerTest {

	@Before
	public void before(){
		Assume.assumeTrue(StackConfiguration.singleton().getDynamoEnabled());
	}
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
		when(nodeDao.getParentRelations(anyLong(),
				eq(DynamoRdsSynchronizer.BATCH_SIZE))).thenReturn(queryResults);

		NodeTreeQueryDao nodeTreeDao = mock(NodeTreeQueryDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("11");

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);

		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		ReflectionTestUtils.setField(sync, "consumer", mock(Consumer.class));

		sync.run();
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
		when(nodeDao.getParentRelations(anyLong(),
				eq(DynamoRdsSynchronizer.BATCH_SIZE))).thenReturn(queryResults);

		NodeTreeQueryDao nodeTreeDao = mock(NodeTreeQueryDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn(null);

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);

		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		ReflectionTestUtils.setField(sync, "consumer", mock(Consumer.class));

		sync.run();
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
		when(nodeDao.getParentRelations(anyLong(),
				eq(DynamoRdsSynchronizer.BATCH_SIZE))).thenReturn(queryResults);

		NodeTreeQueryDao nodeTreeDao = mock(NodeTreeQueryDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("21");

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);

		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		ReflectionTestUtils.setField(sync, "consumer", mock(Consumer.class));

		sync.run();
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
		when(nodeDao.getParentRelations(anyLong(),
				eq(DynamoRdsSynchronizer.BATCH_SIZE))).thenReturn(queryResults);

		NodeTreeQueryDao nodeTreeDao = mock(NodeTreeQueryDao.class);
		when(nodeTreeDao.getParent("1")).thenReturn("11");
		when(nodeTreeDao.isRoot("1")).thenReturn(true);

		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);

		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		ReflectionTestUtils.setField(sync, "consumer", mock(Consumer.class));

		sync.run();
		verify(nodeTreeDao, times(1)).isRoot("1");
	}

	@Test
	public void testEmptyResults() {

		List<NodeParentRelation> results = new ArrayList<NodeParentRelation>(0);
		QueryResults<NodeParentRelation> queryResults = new QueryResults<NodeParentRelation>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(0L);
		NodeDAO nodeDao = mock(NodeDAO.class);
		when(nodeDao.getParentRelations(anyLong(),
				eq(DynamoRdsSynchronizer.BATCH_SIZE))).thenReturn(queryResults);
		NodeTreeQueryDao nodeTreeDao = mock(NodeTreeQueryDao.class);
		NodeTreeUpdateManager nodeTreeUpdateManager = mock(NodeTreeUpdateManager.class);
		DynamoRdsSynchronizer sync = new DynamoRdsSynchronizer(
				nodeDao, nodeTreeDao, nodeTreeUpdateManager);
		ReflectionTestUtils.setField(sync, "consumer", mock(Consumer.class));

		sync.run();
		verify(nodeTreeDao, times(0)).isRoot(anyString());
		verify(nodeTreeUpdateManager, times(0)).create(anyString(), anyString(), any(Date.class));
		verify(nodeTreeUpdateManager, times(0)).update(anyString(), anyString(), any(Date.class));
	}
}
