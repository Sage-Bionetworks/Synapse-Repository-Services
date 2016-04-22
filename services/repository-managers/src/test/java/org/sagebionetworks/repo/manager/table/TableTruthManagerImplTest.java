package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TableTruthManagerImplTest {

	@Mock
	ColumnModelDAO mockColumnModelDao;
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	TableRowTruthDAO mockTableTruthDao;

	TableTruthManagerImpl manager;

	String tableId;

	String columnMd5;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		manager = new TableTruthManagerImpl();
		ReflectionTestUtils.setField(manager, "columnModelDao",
				mockColumnModelDao);
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(manager, "tableTruthDao",
				mockTableTruthDao);

		tableId = "syn123";

		ColumnModel cm = new ColumnModel();
		cm.setId("444");
		List<ColumnModel> columns = Lists.newArrayList(cm);
		when(mockColumnModelDao.getColumnModelsForObject(tableId)).thenReturn(
				columns);
		columnMd5 = TableModelUtils.createSchemaMD5HexCM(columns);

		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
	}

	@Test
	public void testGetSchemaMD5Hex() {
		String md5 = manager.getSchemaMD5Hex(tableId);
		assertEquals(columnMd5, md5);
	}

	@Test
	public void testGetTableTypeTable() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		ObjectType type = manager.getTableType(tableId);
		assertEquals(ObjectType.TABLE, type);
	}

	@Test
	public void testGetTableTypeFileView() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.fileview);
		// call under test
		ObjectType type = manager.getTableType(tableId);
		assertEquals(ObjectType.FILE_VIEW, type);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetTableTypeUnknown() {
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(
				EntityType.project);
		// call under test
		ObjectType type = manager.getTableType(tableId);
		assertEquals(ObjectType.FILE_VIEW, type);
	}

	@Test
	public void testGetTableVersionForTable() {
		TableRowChange lastChange = new TableRowChange();
		lastChange.setRowVersion(999L);
		when(mockTableTruthDao.getLastTableRowChange(tableId)).thenReturn(
				lastChange);
		when(mockNodeDao.getNodeTypeById(tableId)).thenReturn(EntityType.table);
		// call under test
		Long version = manager.getTableVersion(tableId);
		assertEquals(lastChange.getRowVersion(), version);
	}

}
