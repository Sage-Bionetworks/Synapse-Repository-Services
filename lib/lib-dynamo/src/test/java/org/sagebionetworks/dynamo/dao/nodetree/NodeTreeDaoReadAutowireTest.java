package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeTreeDaoReadAutowireTest {

	@Autowired private AmazonDynamoDB dynamoClient;
	@Autowired private DynamoAdminDao dynamoAdminDao;
	@Autowired private NodeTreeUpdateDao nodeTreeUpdateDao;
	@Autowired private NodeTreeQueryDao nodeTreeQueryDao;

	// Map of letters to random IDs
	private Map<String, String> idMap;

	@Before
	public void before() throws Exception {

		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		// Clear dynamo
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);

		this.idMap = DynamoTestUtil.createRandomIdMap(26);
		this.nodeTreeUpdateDao.create(this.idMap.get("a"), this.idMap.get("a"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("b"), this.idMap.get("a"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("e"), this.idMap.get("b"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("f"), this.idMap.get("b"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("u"), this.idMap.get("b"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("h"), this.idMap.get("u"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("i"), this.idMap.get("u"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("j"), this.idMap.get("u"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("k"), this.idMap.get("u"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("l"), this.idMap.get("h"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("m"), this.idMap.get("j"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("n"), this.idMap.get("j"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("c"), this.idMap.get("a"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("d"), this.idMap.get("a"), new Date());
		this.nodeTreeUpdateDao.create(this.idMap.get("g"), this.idMap.get("d"), new Date());

		// Pause for 1.2 seconds to deal with eventual consistency
		// As all the read methods do not use consistent reads
		Thread.sleep(1200L);
	}

	@After
	public void after() {
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
	}

	@Test
	public void test() {

		// testIsRoot()
		{
			Assert.assertTrue(this.nodeTreeQueryDao.isRoot(this.idMap.get("a")));
		}

		// testGetAncestors()
		{
			// Root has 0 ancestors
			List<String> ancestorList = this.nodeTreeQueryDao.getAncestors(this.idMap.get("a"));
			Assert.assertTrue(ancestorList.isEmpty());
			ancestorList = this.nodeTreeQueryDao.getAncestors(this.idMap.get("b"));
			Assert.assertEquals(1, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			ancestorList = this.nodeTreeQueryDao.getAncestors(this.idMap.get("u"));
			Assert.assertEquals(2, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			Assert.assertEquals(this.idMap.get("b"), ancestorList.get(1));
			ancestorList = this.nodeTreeQueryDao.getAncestors(this.idMap.get("m"));
			Assert.assertEquals(4, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			Assert.assertEquals(this.idMap.get("b"), ancestorList.get(1));
			Assert.assertEquals(this.idMap.get("u"), ancestorList.get(2));
			Assert.assertEquals(this.idMap.get("j"), ancestorList.get(3));
			// A node that does not exist has 0 ancestors
			ancestorList = this.nodeTreeQueryDao.getAncestors("fakeNode");
			Assert.assertTrue(ancestorList.isEmpty());
		}

		// testGetParent()
		{
			// Root's parent is the dummy ROOT
			String parent = this.nodeTreeQueryDao.getParent(this.idMap.get("a"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(DboNodeLineage.ROOT, parent);
			// A non-existent node's parent is null
			parent = this.nodeTreeQueryDao.getParent("fakeNode");
			Assert.assertNull(parent);
			parent = this.nodeTreeQueryDao.getParent(this.idMap.get("b"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("a"), parent);
			parent = this.nodeTreeQueryDao.getParent(this.idMap.get("e"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("b"), parent);
			parent = this.nodeTreeQueryDao.getParent(this.idMap.get("u"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("b"), parent);
			parent = this.nodeTreeQueryDao.getParent(this.idMap.get("m"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("j"), parent);
			parent = this.nodeTreeQueryDao.getParent(this.idMap.get("g"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("d"), parent);
		}

		// testGetDescendants()
		{

			List<String> descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 100, null);
			Set<String> descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(14, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("b")));
			Assert.assertTrue(descSet.contains(this.idMap.get("c")));
			Assert.assertTrue(descSet.contains(this.idMap.get("d")));
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));

			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("u"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(7, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));

			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("d"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(1, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));

			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("f"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(0, descSet.size());
		}

		// testGetDescendantsPaging()
		{
			List<String> descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 2, null);
			Assert.assertEquals(2, descList.size());
			Set<String> descSet = new HashSet<String>();
			descSet.addAll(descList);
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 7, descList.get(1));
			Assert.assertEquals(7, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 7, descList.get(6));
			Assert.assertEquals((14 - 2 - 7), descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(14, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("b")));
			Assert.assertTrue(descSet.contains(this.idMap.get("c")));
			Assert.assertTrue(descSet.contains(this.idMap.get("d")));
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));
		}

		// testGetDescendantsGeneration()
		{
			List<String> descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("u"), 1, 100, null);
			Set<String> descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(4, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("u"), 2, 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(3, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
		}

		// testGetDescendantsGenerationPaging()
		{
			List<String> descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 2, 1, null);
			Assert.assertEquals(1, descList.size());
			Set<String> descSet = new HashSet<String>();
			descSet.addAll(descList);
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 2, 1, descList.get(0));
			Assert.assertEquals(1, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 2, 2, descList.get(0));
			Assert.assertEquals(2, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 2, 2, descList.get(1));
			Assert.assertEquals(0, descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(4, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
		}

		// testGetChildren()
		{
			List<String> childList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 1, 2, null);
			Assert.assertEquals(2, childList.size());
			Set<String> childSet = new HashSet<String>();
			childSet.addAll(childList);
			childList = this.nodeTreeQueryDao.getDescendants(this.idMap.get("a"), 1, 2, childList.get(1));
			Assert.assertEquals(1, childList.size());
			childSet.addAll(childList);
			Assert.assertEquals(3, childSet.size());
			Assert.assertTrue(childSet.contains(this.idMap.get("b")));
			Assert.assertTrue(childSet.contains(this.idMap.get("c")));
			Assert.assertTrue(childSet.contains(this.idMap.get("d")));
		}

		// testGetPath()
		{
			List<String> path = this.getPath(this.idMap.get("a"), this.idMap.get("b"));
			Assert.assertEquals(2, path.size());
			Assert.assertEquals(this.idMap.get("a"), path.get(0));
			Assert.assertEquals(this.idMap.get("b"), path.get(1));
			path = this.getPath(this.idMap.get("b"), this.idMap.get("a"));
			Assert.assertEquals(2, path.size());
			Assert.assertEquals(this.idMap.get("a"), path.get(0));
			Assert.assertEquals(this.idMap.get("b"), path.get(1));
			path = this.getPath(this.idMap.get("m"), this.idMap.get("b"));
			Assert.assertEquals(4, path.size());
			Assert.assertEquals(this.idMap.get("b"), path.get(0));
			Assert.assertEquals(this.idMap.get("u"), path.get(1));
			Assert.assertEquals(this.idMap.get("j"), path.get(2));
			Assert.assertEquals(this.idMap.get("m"), path.get(3));
			path = this.getPath(this.idMap.get("d"), this.idMap.get("d"));
			Assert.assertEquals(1, path.size());
			Assert.assertEquals(this.idMap.get("d"), path.get(0));
			path = this.getPath(this.idMap.get("m"), this.idMap.get("k"));
			Assert.assertNull(path);
		}

		// testGetLowestCommonAncestor()
		{
			String anc = this.getLowestCommonAncestor(this.idMap.get("b"), this.idMap.get("d"));
			Assert.assertEquals(this.idMap.get("a"), anc);
			anc = this.getLowestCommonAncestor(this.idMap.get("j"), this.idMap.get("b"));
			Assert.assertEquals(this.idMap.get("b"), anc);
			anc = this.getLowestCommonAncestor(this.idMap.get("e"), this.idMap.get("n"));
			Assert.assertEquals(this.idMap.get("b"), anc);
		}
	}

	/**
	 * The path in-between X and Y. If a path exists between X and Y, both X and Y are returned.
	 * Ancestor is the first in the returned list and descendant is the last in the returned list.
	 * It is up to the user to check the returned list which (X or Y) is the ancestor and which
	 * is the descendant. If X and Y are the same node, only one node (X or Y) is returned in the list.
	 * Null is returned if no path exists between X and Y.
	 */
	private List<String> getPath(String nodeX, String nodeY) throws IncompletePathException {

		if (nodeX.equals(nodeY)) {
			List<String> path = new ArrayList<String>(1);
			path.add(nodeX);
			return path;
		}

		List<String> pathX = this.nodeTreeQueryDao.getAncestors(nodeX);
		List<String> pathY = this.nodeTreeQueryDao.getAncestors(nodeY);;

		// X and Y are not on the same lineage if their depth are the same
		int depthX = pathX.size();
		int depthY = pathY.size();
		if (depthX == depthY) {
			return null;
		}

		// Which is deeper?
		// If X and Y are on the same path, the deeper node is the descendant
		// We walk the deeper path to find the ancestor
		List<String> path = depthX > depthY ? pathX : pathY;
		final String descendant = depthX > depthY ? nodeX : nodeY;
		final String ancestor = depthX > depthY ? nodeY : nodeX;

		List<String> pathInBetween = null;
		for (int i = 0; i < path.size(); i++) {
			String node = path.get(i);
			if (ancestor.equals(node)) {
				pathInBetween = new ArrayList<String>(path.subList(i, path.size()));
				break;
			}
		}

		if (pathInBetween == null) {
			return null;
		}

		List<String> results = new ArrayList<String>(pathInBetween.size() + 1);
		for (int i = 0; i < pathInBetween.size(); i++) {
			results.add(pathInBetween.get(i));
		}
		results.add(descendant);
		return results;
	}

	private String getLowestCommonAncestor(String nodeX, String nodeY) throws IncompletePathException {

		// A special situation where one is the ancestor of another
		List<String> path = this.getPath(nodeX, nodeY);
		if (path != null) {
			return path.get(0);
		}

		List<String> pathX = this.nodeTreeQueryDao.getAncestors(nodeX);
		List<String> pathY = this.nodeTreeQueryDao.getAncestors(nodeY);
		String node = null;
		for (int i = 0; i < pathX.size() && i < pathY.size(); i++) {
			String nX = pathX.get(i);
			String nY = pathY.get(i);
			if (!nX.equals(nY)) {
				break;
			}
			node = nX;
		}
		return node;
	}
}
