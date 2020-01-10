package org.sagebionetworks.repo.model.dbo.trash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dbo.trash.DBOTrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class TrashedEntityUtilsTest {

	@Test
	public void testConvertDboToDtoList() {
		// Test an empty list
		List<DBOTrashedEntity> emptyList = new ArrayList<DBOTrashedEntity>();
		List<TrashedEntity> listBack = TrashedEntityUtils.convertDboToDto(emptyList);
		assertNotNull(listBack);
		assertEquals(0, listBack.size());
		// Test an non-empty list
		DBOTrashedEntity dbo = new DBOTrashedEntity();
		dbo.setId(111L);
		dbo.setNodeName("name");
		dbo.setParentId(222L);
		dbo.setDeletedBy(333L);
		dbo.setDeletedOn(new Timestamp(123456789L));
		assertEquals(Long.valueOf(111L), dbo.getNodeId());
		List<DBOTrashedEntity> dboList = new ArrayList<DBOTrashedEntity>();
		dboList.add(dbo);
		listBack = TrashedEntityUtils.convertDboToDto(dboList);
		assertNotNull(listBack);
		assertEquals(1, listBack.size());
		TrashedEntity trash = listBack.get(0);
		assertEquals(KeyFactory.keyToString(111L), trash.getEntityId());
		assertEquals("name", trash.getEntityName());
		assertEquals(KeyFactory.keyToString(222L), trash.getOriginalParentId());
		assertEquals("333", trash.getDeletedByPrincipalId());
		assertEquals(new Timestamp(123456789L), trash.getDeletedOn());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertDboToDtoListIllegalArgumentException() {
		TrashedEntityUtils.convertDboToDto((List<DBOTrashedEntity>)null);
	}

	@Test
	public void testConvertDboToDto() {
		DBOTrashedEntity dbo = new DBOTrashedEntity();
		dbo.setId(111L);
		dbo.setNodeName("a name");
		dbo.setParentId(222L);
		dbo.setDeletedBy(333L);
		dbo.setDeletedOn(new Timestamp(123456789L));
		assertEquals(Long.valueOf(111L), dbo.getNodeId());
		TrashedEntity trash = TrashedEntityUtils.convertDboToDto(dbo);
		assertEquals(KeyFactory.keyToString(111L), trash.getEntityId());
		assertEquals("a name", trash.getEntityName());
		assertEquals(KeyFactory.keyToString(222L), trash.getOriginalParentId());
		assertEquals("333", trash.getDeletedByPrincipalId());
		assertEquals(new Timestamp(123456789L), trash.getDeletedOn());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertDboToDtoIllegalArgumentException() {
		TrashedEntityUtils.convertDboToDto((DBOTrashedEntity)null);
	}
}
