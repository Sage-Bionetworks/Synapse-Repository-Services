package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class TableIndexConnectionFactoryImplTest {
	
	@Mock
	TableIndexDAO mockTableIndexDAO;
	@Mock
	ConnectionFactory mockDaoConnectionFactory;
	@Mock
	TableManagerSupport mockManagerSupport;
	
	private TableIndexConnectionFactoryImpl indexFactory;
	private IdAndVersion tableId;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		indexFactory = new TableIndexConnectionFactoryImpl();
		ReflectionTestUtils.setField(indexFactory, "connectionFactory", mockDaoConnectionFactory);
		ReflectionTestUtils.setField(indexFactory, "tableManagerSupport", mockManagerSupport);
		tableId = IdAndVersion.parse("syn456");
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
	
	@Test
	public void testGetFirstConnection(){
		when(mockDaoConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDAO);
		TableIndexManager manager = indexFactory.connectToFirstIndex();
		assertNotNull(manager);
	}

}
