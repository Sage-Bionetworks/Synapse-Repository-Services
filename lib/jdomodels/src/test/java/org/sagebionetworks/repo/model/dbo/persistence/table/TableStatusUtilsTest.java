package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;

public class TableStatusUtilsTest {
	
	@Test
	public void testRoundTrip(){
		TableStatus dto = new TableStatus();
		dto.setStartedOn(new Date(1));
		dto.setChangedOn(new Date(2));
		dto.setErrorDetails("This is the longer error details");
		dto.setErrorMessage("This is the short message");
		dto.setTableId("123");
		dto.setVersion(456L);
		dto.setProgressCurrent(50L);
		dto.setProgressMessage("Making progress");
		dto.setProgressTotal(100L);
		dto.setState(TableState.PROCESSING_FAILED);
		dto.setTotalTimeMS(10L);
		dto.setResetToken("reset token");
		dto.setLastTableChangeEtag("last etag");
		
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
	
	@Test
	public void testAbbreviateOverLimit() {
		TableStatus dto = new TableStatus();
		dto.setErrorMessage(StringUtils.repeat('a', TableStatusUtils.MAX_CHARS+1));
		dto.setProgressMessage(StringUtils.repeat('a', TableStatusUtils.MAX_CHARS+1));
		// call under test
		DBOTableStatus dbo = TableStatusUtils.createDBOFromDTO(dto);
		// error
		assertNotNull(dbo.getErrorMessage());
		assertEquals(TableStatusUtils.MAX_CHARS, dbo.getErrorMessage().length());
		assertTrue(dbo.getErrorMessage().endsWith("..."));
		// progress
		assertNotNull(dbo.getProgressMessage());
		assertEquals(TableStatusUtils.MAX_CHARS, dbo.getProgressMessage().length());
		assertTrue(dbo.getErrorMessage().endsWith("..."));
	}
	
	@Test
	public void testAbbreviateOverAtLimit() {
		TableStatus dto = new TableStatus();
		dto.setErrorMessage(StringUtils.repeat('a', TableStatusUtils.MAX_CHARS));
		dto.setProgressMessage(StringUtils.repeat('a', TableStatusUtils.MAX_CHARS));
		// call under test
		DBOTableStatus dbo = TableStatusUtils.createDBOFromDTO(dto);
		// error
		assertNotNull(dbo.getErrorMessage());
		assertEquals(TableStatusUtils.MAX_CHARS, dbo.getErrorMessage().length());
		assertFalse(dbo.getErrorMessage().endsWith("..."));
		// progress
		assertNotNull(dbo.getProgressMessage());
		assertEquals(TableStatusUtils.MAX_CHARS, dbo.getProgressMessage().length());
		assertFalse(dbo.getErrorMessage().endsWith("..."));
	}

}
