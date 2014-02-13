package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;

public class TableStatusUtilsTest {
	
	@Test
	public void testRoundTrip(){
		TableStatus dto = new TableStatus();
		dto.setChangedOn(new Date(1));
		dto.setErrorDetails("This is the longer error details");
		dto.setErrorMessage("This is the short message");
		dto.setTableId("123");
		dto.setProgresssCurrent(50L);
		dto.setProgresssMessage("Making progress");
		dto.setProgresssTotal(100L);
		dto.setState(TableState.PROCESSING_FAILED);
		dto.setTotalTimeMS(10L);
		
		// to the DBO 
		DBOTableStatus dbo = TableStatusUtils.createDBOFromDTO(dto);
		assertNotNull(dbo);
		TableStatus clone = TableStatusUtils.createDTOFromDBO(dbo);
		assertEquals(dto, clone);
	}

	@Test
	public void testAllTypes(){
		for(TableState state: TableState.values()){
			TableStatus dto = new TableStatus();
			dto.setState(state);
			// to the DBO 
			DBOTableStatus dbo = TableStatusUtils.createDBOFromDTO(dto);
			assertNotNull(dbo);
			TableStatus clone = TableStatusUtils.createDTOFromDBO(dbo);
			assertEquals(dto, clone);
		}
	}
}
