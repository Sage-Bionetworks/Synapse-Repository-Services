package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class TableIndexConnectionFactoryImplTest {
	
	private TableIndexDAO mockTableIndexDAO;
	private ConnectionFactory mockDaoConnectionFactory;
	private TableIndexConnectionFactoryImpl indexFactory;
	private String tableId;
	
	@Before
	public void before(){
		mockTableIndexDAO = Mockito.mock(TableIndexDAO.class);
		mockDaoConnectionFactory = Mockito.mock(ConnectionFactory.class);
		indexFactory = new TableIndexConnectionFactoryImpl();
		ReflectionTestUtils.setField(indexFactory, "connectionFactory", mockDaoConnectionFactory);
		tableId = "syn456";
		when(mockDaoConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testConnectToTableIndex(){
		indexFactory.connectToTableIndex(null);
	}
	
	@Test
	public void testConnectToTableIndexHappy(){
		TableIndexManager manager = indexFactory.connectToTableIndex(tableId);
		assertNotNull(manager);
	}
	
	@Test (expected=TableIndexConnectionUnavailableException.class)
	public void testConnectToTableUnavailabl(){
		when(mockDaoConnectionFactory.getConnection(tableId)).thenReturn(null);
		indexFactory.connectToTableIndex(tableId);
	}

}
