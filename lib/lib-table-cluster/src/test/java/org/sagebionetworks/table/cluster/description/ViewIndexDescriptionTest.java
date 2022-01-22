package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;

public class ViewIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSql() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		// call under test
		String sql = vid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T999( " + "ROW_ID BIGINT NOT NULL, " + "ROW_VERSION BIGINT NOT NULL, "
				+ "ROW_ETAG varchar(36) NOT NULL, " + "ROW_BENEFACTOR BIGINT NOT NULL, " + "PRIMARY KEY (ROW_ID), "
				+ "KEY `IDX_ETAG` (ROW_ETAG), " + "KEY `IDX_BENEFACTOR` (ROW_BENEFACTOR))", sql);
	}

	@Test
	public void testGetBenefactorColumnNamesWithEntityView() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		// call under test
		assertEquals(
				Collections.singletonList(new BenefactorDescription(TableConstants.ROW_BENEFACTOR, ObjectType.ENTITY)),
				vid.getBenefactors());
	}
	
	@Test
	public void testGetBenefactorColumnNamesWithDataset() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		// call under test
		assertEquals(
				Collections.singletonList(new BenefactorDescription(TableConstants.ROW_BENEFACTOR, ObjectType.ENTITY)),
				vid.getBenefactors());
	}

	@Test
	public void testGetBenefactorColumnNamesWithSubmissionView() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.submissionview);
		// call under test
		assertEquals(
				Collections
						.singletonList(new BenefactorDescription(TableConstants.ROW_BENEFACTOR, ObjectType.EVALUATION)),
				vid.getBenefactors());
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQuery() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		// call under test
		List<String> result = vid.getColumnNamesToAddToSelect(SqlType.query);
		assertEquals(Arrays.asList(TableConstants.ROW_ID, TableConstants.ROW_VERSION, TableConstants.ROW_ETAG), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuild() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			vid.getColumnNamesToAddToSelect(SqlType.build);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for views", message);
	}
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			vid.getColumnNamesToAddToSelect(null);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for views", message);
	}
}
