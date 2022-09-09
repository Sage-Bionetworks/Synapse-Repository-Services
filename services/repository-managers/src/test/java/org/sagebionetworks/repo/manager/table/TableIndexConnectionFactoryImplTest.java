package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TableIndexConnectionFactoryImplTest {
	
	@Mock
	private TableIndexManager mockIndexManager;
	
	@InjectMocks
	private TableIndexConnectionFactoryImpl indexFactory;
	
	
	@Test
	public void testConnectToTableIndex(){
		assertThrows(IllegalArgumentException.class, () -> {
			indexFactory.connectToTableIndex(null);
		});
	}
		
	@Test
	public void testGetFirstConnection(){
		TableIndexManager manager = indexFactory.connectToFirstIndex();
		assertEquals(mockIndexManager, manager);
	}

}
