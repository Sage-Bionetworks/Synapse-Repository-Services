package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.table.ColumnProvenanceDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnProvenance;
import org.sagebionetworks.repo.model.table.ColumnProvenanceEntry;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ColumnProvenanceDaoImplTest {

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeDaoObjectHelper nodeHelper;

	@Autowired
	private ColumnProvenanceDao dao;

	private IdAndVersion objectId;

	@BeforeEach
	public void before() {
		nodeDao.truncateAll();
		dao.truncateAll();

		String nodeId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.materializedview);
		}).getId();

		objectId = KeyFactory.idAndVersion(nodeId, null);
	}

	@AfterEach
	public void after() {
		dao.truncateAll();
		nodeDao.truncateAll();
	}

	private ColumnProvenance provenanceFor(IdAndVersion object, String setFunctionType) {
		return new ColumnProvenance().setObjectId("syn" + object.getId())
				.setVersionNumber(object.getVersion().orElse(null))
				.setColumns(Arrays.asList(
						new ColumnProvenanceEntry().setOutputColumnId("501").setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(new SourceColumnReference()
										.setSourceObjectId("syn123").setSourceVersionNumber(3L).setSourceColumnId("111"))),
						new ColumnProvenanceEntry().setOutputColumnId("502").setDerivationKind(DerivationKind.AGGREGATE)
								.setSetFunctionType(setFunctionType)
								.setInputs(Collections.singletonList(new SourceColumnReference()
										.setSourceObjectId("syn123").setSourceColumnId("222")))));
	}

	@Test
	public void testSaveGetClear() {
		ColumnProvenance provenance = provenanceFor(objectId, "COUNT");

		// call under test
		dao.saveColumnProvenance(objectId, provenance);

		// call under test - full JSON round-trip.
		assertEquals(Optional.of(provenance), dao.getColumnProvenance(objectId));

		// call under test
		dao.clear(objectId);
		assertEquals(Optional.empty(), dao.getColumnProvenance(objectId));
	}

	@Test
	public void testSaveUpdatesExisting() {
		dao.saveColumnProvenance(objectId, provenanceFor(objectId, "COUNT"));

		ColumnProvenance updated = provenanceFor(objectId, "MAX");

		// call under test - a re-save for the same object version replaces the document.
		dao.saveColumnProvenance(objectId, updated);

		assertEquals(Optional.of(updated), dao.getColumnProvenance(objectId));
	}

	@Test
	public void testGetWithNoRow() {
		// call under test
		assertEquals(Optional.empty(), dao.getColumnProvenance(objectId));
	}

	@Test
	public void testDeletingNodeCascadesProvenance() {
		dao.saveColumnProvenance(objectId, provenanceFor(objectId, "COUNT"));
		assertEquals(Optional.of(provenanceFor(objectId, "COUNT")), dao.getColumnProvenance(objectId));

		// call under test - the FK ON DELETE CASCADE removes the derived provenance with its owner.
		nodeDao.delete(KeyFactory.keyToString(objectId.getId()));

		assertEquals(Optional.empty(), dao.getColumnProvenance(objectId));
	}

	@Test
	public void testTruncateAll() {
		dao.saveColumnProvenance(objectId, provenanceFor(objectId, "COUNT"));

		// call under test
		dao.truncateAll();

		assertEquals(Optional.empty(), dao.getColumnProvenance(objectId));
	}

	@Test
	public void testSaveWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.saveColumnProvenance(null, provenanceFor(objectId, "COUNT"));
		}).getMessage();
		assertEquals("object is required.", message);
	}

	@Test
	public void testSaveWithNullProvenance() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.saveColumnProvenance(objectId, null);
		}).getMessage();
		assertEquals("provenance is required.", message);
	}

	@Test
	public void testGetWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getColumnProvenance(null);
		}).getMessage();
		assertEquals("object is required.", message);
	}

	@Test
	public void testClearWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.clear(null);
		}).getMessage();
		assertEquals("object is required.", message);
	}

}
