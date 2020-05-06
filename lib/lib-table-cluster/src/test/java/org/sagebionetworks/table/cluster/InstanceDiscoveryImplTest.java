package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;

@ExtendWith(MockitoExtension.class)
public class InstanceDiscoveryImplTest {
	
	@Mock
	private StackConfiguration mockConfiguration;

	@InjectMocks
	private InstanceDiscoveryImpl discovery;
	
	@BeforeEach
	public void before(){		
		when(mockConfiguration.getTablesDatabaseCount()).thenReturn(2);
		when(mockConfiguration.getTablesDatabaseEndpointForIndex(0)).thenReturn("endpoint0");
		when(mockConfiguration.getTablesDatabaseSchemaForIndex(0)).thenReturn("schema0");
		when(mockConfiguration.getTablesDatabaseEndpointForIndex(1)).thenReturn("endpoint1");
		when(mockConfiguration.getTablesDatabaseSchemaForIndex(1)).thenReturn("schema1");
	}
	
	@Test
	public void testDiscoverAllInstances(){
		List<InstanceInfo> list = discovery.discoverAllInstances();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("jdbc:mysql://endpoint0/schema0?rewriteBatchedStatements=true",list.get(0).getUrl());
		assertEquals("jdbc:mysql://endpoint1/schema1?rewriteBatchedStatements=true",list.get(1).getUrl());
	}
	
}
