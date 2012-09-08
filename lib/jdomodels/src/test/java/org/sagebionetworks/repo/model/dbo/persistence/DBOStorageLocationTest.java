package org.sagebionetworks.repo.model.dbo.persistence;

import org.junit.Test;

public class DBOStorageLocationTest {

	@Test(expected=NullPointerException.class)
	public void testSetId() {
		DBOStorageLocation sl = new DBOStorageLocation();
		sl.setId(null);
	}

	@Test(expected=NullPointerException.class)
	public void testSetNodeId() {
		DBOStorageLocation sl = new DBOStorageLocation();
		sl.setNodeId(null);
	}

	@Test(expected=NullPointerException.class)
	public void testSetUserId() {
		DBOStorageLocation sl = new DBOStorageLocation();
		sl.setUserId(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetLocation() {
		DBOStorageLocation sl = new DBOStorageLocation();
		sl.setLocation(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetStorageProvider() {
		DBOStorageLocation sl = new DBOStorageLocation();
		sl.setStorageProvider(null);
	}
}
