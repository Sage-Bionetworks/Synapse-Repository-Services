package org.sagebionetworks.repo.manager.agent.handler.grid;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.update.OnMatchFailure;
import org.sagebionetworks.repo.model.grid.update.OnMissingValue;
import org.sagebionetworks.repo.model.grid.update.TemplateSetValue;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class TemplateSetValueProcessorTest {

	private TemplateSetValueProcessor processor;
	private RowView row;

	@BeforeEach
	public void before() {
		processor = new TemplateSetValueProcessor();
		row = new RowView();
	}

	@Test
	public void testCreateConWithCombineExtractTransform() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}/{size}")
				.setPattern("participant-(\\d+)/(\\w+)").setReplacement("$2^$1");
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(new RowData()
				.setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\", \"size\":\"medium\"}"))));
		// call under test
		Optional<ConValue> con = processor.createConValue(row, sv, svRaw);

		Optional<ConValue> expected = Optional.of(new ConValue(ConType.STRING, "medium^123"));
		assertEquals(expected, con);
	}

	@Test
	public void testCreateConWithOnMissingValueDefault() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId")
				.setSourceTemplate("{name}/{missing}").setOnMissingValue(null);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\", \"missing\":null}"))));

		// call under test
		Optional<ConValue> result = processor.createConValue(row, sv, svRaw);
		// default to set null.
		assertEquals(Optional.of(new ConValue(ConType.NULL, null)), result);
	}

	@ParameterizedTest
	@EnumSource(OnMissingValue.class)
	public void testCreateConWithOnMissingValueAndNull(OnMissingValue onMissingValue) {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId")
				.setSourceTemplate("{name}/{missing}").setOnMissingValue(onMissingValue);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\", \"missing\":null}"))));

		Optional<ConValue> result = processor.createConValue(row, sv, svRaw);

		switch (onMissingValue) {
		case SET_NULL:
			assertEquals(Optional.of(new ConValue(ConType.NULL, null)), result);
			break;
		case SKIP_UPDATE:
			assertEquals(Optional.empty(), result);
			break;
		case USE_EMPTY_STRING:
			assertEquals(Optional.of(new ConValue(ConType.STRING, "participant-123/")), result);
			break;
		}
	}

	@ParameterizedTest
	@EnumSource(OnMissingValue.class)
	public void testCreateConWithOnMissingValueAndNotDefined(OnMissingValue onMissingValue) {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId")
				.setSourceTemplate("{name}/{missing}").setOnMissingValue(onMissingValue);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject()
				.setData(new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\"}"))));

		Optional<ConValue> result = processor.createConValue(row, sv, svRaw);

		switch (onMissingValue) {
		case SET_NULL:
			assertEquals(Optional.of(new ConValue(ConType.NULL, null)), result);
			break;
		case SET_UNDEFINED:
			assertEquals(Optional.of(new ConValue(ConType.UNDEFINED, null)), result);
			break;
		case SKIP_UPDATE:
			assertEquals(Optional.empty(), result);
			break;
		case USE_EMPTY_STRING:
			assertEquals(Optional.of(new ConValue(ConType.STRING, "participant-123/")), result);
			break;
		default:
			assertTrue(false);
		}
	}

	@Test
	public void testCreateConWithExtractLong() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}")
				.setPattern("participant-(\\d+)").setReplacement("$1");
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject()
				.setData(new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\"}"))));
		// call under test
		Optional<ConValue> con = processor.createConValue(row, sv, svRaw);

		Optional<ConValue> expected = Optional.of(new ConValue(ConType.LONG, 123L));
		assertEquals(expected, con);
	}

	@Test
	public void testCreateConWithExtractAndReplacementNull() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}")
				.setPattern("participant-(\\d+)").setReplacement(null);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject()
				.setData(new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"participant-123\"}"))));
		// call under test
		Optional<ConValue> con = processor.createConValue(row, sv, svRaw);

		Optional<ConValue> expected = Optional.of(new ConValue(ConType.LONG, 123L));
		assertEquals(expected, con);
	}

	@Test
	public void testCreateConWithMatchFailureNull() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}/{size}")
				.setPattern("participant-(\\d+)/(\\w+)").setReplacement("$2^$1").setOnMatchFailure(null);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"other-123\", \"size\":\"medium\"}"))));
		// call under test
		Optional<ConValue> con = processor.createConValue(row, sv, svRaw);

		assertEquals(Optional.of(new ConValue(ConType.NULL, null)), con);
		;
	}

	@ParameterizedTest
	@EnumSource(OnMatchFailure.class)
	public void testCreateConWithMatchFailure(OnMatchFailure onMatchFailure) {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}/{size}")
				.setPattern("participant-(\\d+)/(\\w+)").setReplacement("$2^$1").setOnMatchFailure(onMatchFailure);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"other-123\", \"size\":\"medium\"}"))));
		// call under test
		Optional<ConValue> con = processor.createConValue(row, sv, svRaw);

		switch (onMatchFailure) {
		case SET_NULL:
			assertEquals(Optional.of(new ConValue(ConType.NULL, null)), con);
			break;
		case SKIP_UPDATE:
			assertEquals(Optional.empty(), con);
			break;
		case SET_UNDEFINED:
			assertEquals(Optional.of(new ConValue(ConType.UNDEFINED, null)), con);
			break;
		default:
			assertTrue(false);
		}
	}

	@Test
	public void testCreateConWithNullRow() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate("{name}/{size}")
				.setPattern("participant-(\\d+)/(\\w+)").setReplacement("$2^$1").setOnMatchFailure(null);
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.createConValue(row, sv, svRaw);
		}).getMessage();
		assertEquals("row is required.", message);
	}

	@Test
	public void testCreateConWithNullSetValue() {
		TemplateSetValue sv = null;
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"other-123\", \"size\":\"medium\"}"))));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.createConValue(row, sv, null);
		}).getMessage();
		assertEquals("TemplateSetValue is required.", message);
	}

	@Test
	public void testCreateConWithNullTempalte() {
		TemplateSetValue sv = new TemplateSetValue().setColumnName("participantId").setSourceTemplate(null)
				.setPattern("participant-(\\d+)/(\\w+)").setReplacement("$2^$1");
		JSONObject svRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(sv);
		row.setRowObject(new RowObject().setData(
				new RowData().setRowJsonDocument(new JSONObject("{\"name\":\"other-123\", \"size\":\"medium\"}"))));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.createConValue(row, sv, svRaw);
		}).getMessage();
		assertEquals("TemplateSetValue.sourceTemplate is required.", message);
	}
}
