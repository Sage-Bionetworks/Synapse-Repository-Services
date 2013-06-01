package org.sagebionetworks.repo.web.service;

import org.junit.Assert;
import org.junit.Test;

public class NodeTreeQueryServiceImplTest {

	private NodeTreeQueryService service = new NodeTreeQueryServiceImpl();

	@Test(expected=IllegalArgumentException.class)
	public void testGetAncestorsWithException1() {
		this.service.getAncestors(null, "node");
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetAncestorsWithException2() {
		this.service.getAncestors("user", null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetParentWithException1() {
		this.service.getParent(null, "node");
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetParentWithException2() {
		this.service.getParent("user", null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsWithException1() {
		this.service.getDescendants(null, "node", 2, null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsWithException2() {
		this.service.getDescendants("user", null, 2, null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsWithException3() {
		this.service.getDescendants(null, "node", 1, 2, null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsWithException4() {
		this.service.getDescendants("user", null, 1, 2, null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetChildrenWithException1() {
		this.service.getChildren(null, "node", 2, null);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetChildrenWithException2() {
		this.service.getChildren("user", null, 2, null);
		Assert.fail();
	}
}
