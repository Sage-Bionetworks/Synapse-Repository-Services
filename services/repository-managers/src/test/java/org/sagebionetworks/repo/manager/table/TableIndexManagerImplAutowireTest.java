package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableIndexManagerImplAutowireTest {
	
	
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	
	TableIndexManagerImpl tableIndexManager;
	IdAndVersion tableId;
	
	@BeforeEach
	public void before() {
		tableIndexManager = (TableIndexManagerImpl) connectionFactory.connectToFirstIndex();
		
		tableId = IdAndVersion.parse("syn123");
	}
	
	@AfterEach
	public void after() {
		tableIndexManager.deleteTableIndex(tableId);
	}
	
	
	@Test
	public void testSetIndexSchemaWithUpdate() {
		
		ColumnModel one = new ColumnModel();
		one.setId("44");
		one.setColumnType(ColumnType.BOOLEAN);
		List<ColumnModel> schema = List.of(one);
		
		ColumnModel two = new ColumnModel();
		two.setId("55");
		two.setColumnType(ColumnType.BOOLEAN);
		
		tableIndexManager.deleteTableIndex(tableId);
		
		assertEquals(Collections.emptyList(), tableIndexManager.getCurrentTableSchema(tableId));

		// call under test
		tableIndexManager.setIndexSchema(new TableIndexDescription(tableId), schema);
		
		assertEquals(schema, tableIndexManager.getCurrentTableSchema(tableId));
		
		schema = List.of(one, two);
		
		// call under test
		tableIndexManager.setIndexSchema(new TableIndexDescription(tableId), schema);
		
		assertEquals(schema, tableIndexManager.getCurrentTableSchema(tableId));
		
		tableIndexManager.deleteTableIndex(tableId);
		
		assertEquals(Collections.emptyList(), tableIndexManager.getCurrentTableSchema(tableId));
	}

}
