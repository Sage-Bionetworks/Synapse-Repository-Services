package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableStatusDAOImplTest {

	@Autowired
	TableStatusDAO tableStatusDAO;
	
	@Test
	public void testCreateAndGet() throws NotFoundException{
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
		
		// create it
		TableStatus clone = tableStatusDAO.createOrUpdateTableStatus(dto);
		assertNotNull(clone);
		assertEquals(dto, clone);
		// Update it
		dto.setState(TableState.AVAILABLE_FOR_QUERY);
		clone = tableStatusDAO.createOrUpdateTableStatus(dto);
		assertNotNull(clone);
		assertEquals(dto, clone);
		// Get it
		clone = tableStatusDAO.getTableStatus("123");
		assertNotNull(clone);
		assertEquals(dto, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws NotFoundException{
		TableStatus clone = tableStatusDAO.getTableStatus("-99");
	}
}
