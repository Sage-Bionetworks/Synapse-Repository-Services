package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.LinkedList;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.table.TableRowChange;

import com.google.common.collect.Lists;

/**
 * 
 * @author jmhill
 *
 */
public class TableRowChangeUtilsTest {
	@Test
	public void testDTOandDBORoundTrip(){
		TableRowChange dto = new TableRowChange();
		dto.setTableId("syn123");
		dto.setRowVersion(12l);
		dto.setCreatedBy("456");
		dto.setCreatedOn(new Date(101));
		dto.setIds(Lists.newArrayList(111L, 222L));
		dto.setBucket("bucket");
		dto.setKey("key");
		dto.setEtag("someEtag");
		dto.setRowCount(999L);
		// To DBO
		DBOTableRowChange dbo = TableRowChangeUtils.createDBOFromDTO(dto);
		assertNotNull(dbo);
		// Create a clone
		TableRowChange clone = TableRowChangeUtils.ceateDTOFromDBO(dbo);
		assertNotNull(clone);
		assertEquals(dto, clone);
	}
}
