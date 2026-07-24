package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

@ExtendWith(MockitoExtension.class)
public class InPlaceAnnotationSourceWriterTest {

	@Mock
	private UserInfo mockUser;
	@Mock
	private AnnotationWriter mockAnnotationWriter;
	@Mock
	private AnnotationsTranslator mockAnnotationsTranslator;

	@InjectMocks
	private InPlaceAnnotationSourceWriter writer;

	@Test
	public void testCanAddRemoveRows() {
		assertFalse(writer.canAddRemoveRows());
	}

	@Test
	public void testCanAddRemoveColumns() {
		assertFalse(writer.canAddRemoveColumns());
	}

	@Test
	public void testAddNewRowToSource() {
		// call under test
		writer.addNewRowToSource(new RowSourceItem(new TreeMap<String, ConValue>(), "theKey"));
		assertEquals(List.of("Cannot add the row: 'theKey' to a source view."), writer.getErrorMessages());
	}

	@Test
	public void testRemoveRow() {
		// call under test
		writer.removeRow(new RowSourceItem(new TreeMap<String, ConValue>(), "theKey"));
		assertEquals(List.of("Cannot remove the row: 'theKey' from a source view."), writer.getErrorMessages());
	}

	@Test
	public void testAddColumnToSource() {
		// call under test
		writer.addColumnToSource("one");
		assertEquals(List.of("Cannot add the column: 'one' to a source view."), writer.getErrorMessages());
	}

	@Test
	public void testRemoveColumn() {
		// call under test
		writer.removeColumn("one");
		assertEquals(List.of("Cannot remove the column: 'one' from a source view."), writer.getErrorMessages());
	}

	@Test
	public void testTranslateCellChangesWithNullValue() {
		Map<String, ConValue> changes = new HashMap<>();
		changes.put("aString", null);

		// call under test
		Map<String, AnnotationsValue> results = writer.translateCellChanges(changes);
		Map<String, AnnotationsValue> expected = new HashMap<>();
		expected.put("aString", null);
		assertEquals(expected, results);
	}

	@Test
	public void testTranslateCellChangesWithUndefinedValue() {
		Map<String, ConValue> changes = new HashMap<>();
		changes.put("aString", new ConValue(ConType.UNDEFINED, null));

		// call under test
		Map<String, AnnotationsValue> results = writer.translateCellChanges(changes);
		Map<String, AnnotationsValue> expected = new HashMap<>();
		expected.put("aString", null);
		assertEquals(expected, results);
	}

	@Test
	public void testTranslateCellChangesWithJSONNull() {
		Map<String, ConValue> changes = new HashMap<>();
		changes.put("aString", new ConValue(ConType.NULL, null));

		// call under test
		Map<String, AnnotationsValue> results = writer.translateCellChanges(changes);
		Map<String, AnnotationsValue> expected = new HashMap<>();
		expected.put("aString", null);
		assertEquals(expected, results);
	}

	@Test
	public void testTranslateCellChangesWithValueNull() {
		Map<String, ConValue> changes = new HashMap<>();
		changes.put("aString", new ConValue(ConType.STRING, null));

		// call under test
		Map<String, AnnotationsValue> results = writer.translateCellChanges(changes);
		Map<String, AnnotationsValue> expected = new HashMap<>();
		expected.put("aString", null);
		assertEquals(expected, results);
	}

	@Test
	public void testTranslateCellChangesValidValues() {
		Map<String, ConValue> changes = Map.of("aString", new ConValue(ConType.STRING, "one"), "anInt",
				new ConValue(ConType.LONG, 222L));

		Map<String, AnnotationsValue> expected = Map.of("aString",
				new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("one")), "anInt",
				new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("222")));

		when(mockAnnotationsTranslator.getAnnotationValueFromJsonObject(eq("aString"),
				argThat(json -> json != null && json.toString().equals("{\"aString\":\"one\"}"))))
				.thenReturn(expected.get("aString"));
		when(mockAnnotationsTranslator.getAnnotationValueFromJsonObject(eq("anInt"),
				argThat(json -> json != null && json.toString().equals("{\"anInt\":222}"))))
				.thenReturn(expected.get("anInt"));

		// call under test
		Map<String, AnnotationsValue> results = writer.translateCellChanges(changes);

		assertEquals(expected, results);
	}

	@Test
	public void testApplyCellChangesFromCopyToSource() {
		Map<String, ConValue> changes = Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
				new ConValue(ConType.LONG, 222L));

		InPlaceAnnotationSourceWriter spy = Mockito.spy(writer);

		Map<String, AnnotationsValue> expected = Map.of("aString",
				new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("one")), "anInt",
				new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("222")));

		doReturn(expected).when(spy).translateCellChanges(changes);

		// call under test
		spy.applyCellChangesFromCopyToSource("syn123", changes);

		verify(mockAnnotationWriter).updateChangedAnnotations(mockUser, "syn123", expected);
	}

	@Test
	public void testApplyCellChangesFromCopyToSourceWithIllegalArgument() {
		Map<String, ConValue> changes = Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
				new ConValue(ConType.LONG, 222L));

		InPlaceAnnotationSourceWriter spy = Mockito.spy(writer);

		doThrow(new IllegalArgumentException("bad value")).when(spy).translateCellChanges(changes);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			spy.applyCellChangesFromCopyToSource("syn123", changes);
		}).getMessage();
		assertEquals("bad value", message);
		assertEquals(List.of("Failed to update row: 'syn123' in the source view.  Error message: bad value"),
				spy.getErrorMessages());
	}

}
