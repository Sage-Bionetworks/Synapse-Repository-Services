package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;

@ExtendWith(MockitoExtension.class)
public class TableIndexConnectionFactoryImplTest {
	
	@Mock
	private TableIndexDAO mockTableIndexDAO;
	@Mock
	private ConnectionFactory mockDaoConnectionFactory;
	@Mock
	private TableManagerSupport mockManagerSupport;
	@Mock
	private MetadataIndexProviderFactory mockMetaDataIndexProviderFactory;
	@Mock
	private ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	
	@InjectMocks
	private TableIndexConnectionFactoryImpl indexFactory;
	
	private IdAndVersion tableId;
	
	@BeforeEach
	public void before(){
		tableId = IdAndVersion.parse("syn456");
	}
	
	@Test
	public void testConnectToTableIndex(){
		assertThrows(IllegalArgumentException.class, () -> {
			indexFactory.connectToTableIndex(null);
		});
	}
	
	@Test
	public void testConnectToTableIndexHappy(){
		when(mockDaoConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		TableIndexManager manager = indexFactory.connectToTableIndex(tableId);
		assertNotNull(manager);
	}
	
	@Test
	public void testConnectToTableUnavailabl(){
		when(mockDaoConnectionFactory.getConnection(tableId)).thenReturn(null);
		
		assertThrows(TableIndexConnectionUnavailableException.class, () -> {
			indexFactory.connectToTableIndex(tableId);
		});
	}
	
	@Test
	public void testGetFirstConnection(){
		when(mockDaoConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDAO);
		TableIndexManager manager = indexFactory.connectToFirstIndex();
		assertNotNull(manager);
	}

}
