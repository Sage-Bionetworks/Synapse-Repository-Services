package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class UpdateMetadataChangeTest {

	private LogicalTimestamp objectId;
	private LogicalTimestamp metadataId;
	private JSONObject validation;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		objectId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		metadataId = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
		validation = EntityFactory.createJSONObjectForEntity(new ValidationResults().setIsValid(true));
	}

	@Test
	public void testToAndFromJson() {

		UpdateMetadataChange change = new UpdateMetadataChange().setRowMetadataId(metadataId).setRowObjectId(objectId)
				.setValidationState(validation);
		// call under test
		JSONObject json = change.toJson();
		assertEquals("{\"o\":[1,2],\"m\":[3,4],\"state\":{\"isValid\":true}}", json.toString());
		// call under test
		UpdateMetadataChange clone = new UpdateMetadataChange(json);
		assertEquals(change, clone);
	}

	@Test
	public void testToAndFromJsonWithNullMetadataId() {

		UpdateMetadataChange change = new UpdateMetadataChange().setRowMetadataId(null).setRowObjectId(objectId)
				.setValidationState(validation);
		// call under test
		JSONObject json = change.toJson();
		assertEquals("{\"o\":[1,2],\"state\":{\"isValid\":true}}", json.toString());
		// call under test
		UpdateMetadataChange clone = new UpdateMetadataChange(json);
		assertEquals(change, clone);
	}

	@Test
	public void testToAndFromJsonWithNullObjectId() {

		UpdateMetadataChange change = new UpdateMetadataChange().setRowMetadataId(metadataId).setRowObjectId(null)
				.setValidationState(validation);
		// call under test
		JSONObject json = change.toJson();
		assertEquals("{\"m\":[3,4],\"state\":{\"isValid\":true}}", json.toString());
		// call under test
		UpdateMetadataChange clone = new UpdateMetadataChange(json);
		assertEquals(change, clone);
	}

	@Test
	public void testToAndFromJsonWithNullValidation() {

		UpdateMetadataChange change = new UpdateMetadataChange().setRowMetadataId(metadataId).setRowObjectId(objectId)
				.setValidationState(null);
		// call under test
		JSONObject json = change.toJson();
		assertEquals("{\"o\":[1,2],\"m\":[3,4]}", json.toString());
		// call under test
		UpdateMetadataChange clone = new UpdateMetadataChange(json);
		assertEquals(change, clone);
	}

	@Test
	public void testToAndFromJsonWithAlllNull() {

		UpdateMetadataChange change = new UpdateMetadataChange();
		// call under test
		JSONObject json = change.toJson();
		assertEquals("{}", json.toString());
		// call under test
		UpdateMetadataChange clone = new UpdateMetadataChange(json);
		assertEquals(change, clone);
	}

}
