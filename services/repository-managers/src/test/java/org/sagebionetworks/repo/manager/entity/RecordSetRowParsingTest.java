package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

public class RecordSetRowParsingTest {

	private static final CsvTableDescriptor TAB_SEPARATED = new CsvTableDescriptor().setIsFirstLineHeader(true)
			.setSeparator("\t");
	private static final CsvTableDescriptor EVERY_DEFAULT_SPELLED_OUT = new CsvTableDescriptor()
			.setIsFirstLineHeader(true).setSeparator(",").setQuoteCharacter("\"").setEscapeCharacter("\\")
			.setLineEnd("\n");

	@Test
	public void testOfWithRecordSet() {
		RecordSet recordSet = new RecordSet().setUpsertKey(List.of("a")).setCsvDescriptor(TAB_SEPARATED);

		// call under test
		RecordSetRowParsing result = RecordSetRowParsing.of(recordSet);

		assertEquals(new RecordSetRowParsing(List.of("a"), TAB_SEPARATED), result);
	}

	@Test
	public void testOfWithNode() {
		Node node = new Node();
		node.setUpsertKey(List.of("a"));
		node.setCsvDescriptor(TAB_SEPARATED);

		// call under test
		RecordSetRowParsing result = RecordSetRowParsing.of(node);

		assertEquals(new RecordSetRowParsing(List.of("a"), TAB_SEPARATED), result);
	}

	@Test
	public void testIsUpsertKeyChangedFromWithSameKey() {
		// call under test
		assertFalse(parsing(List.of("a", "b"), null).isUpsertKeyChangedFrom(parsing(List.of("a", "b"), null)));
	}

	@Test
	public void testIsUpsertKeyChangedFromWithNullOnBothSides() {
		// call under test
		assertFalse(parsing(null, null).isUpsertKeyChangedFrom(parsing(null, null)));
	}

	@Test
	public void testIsUpsertKeyChangedFromWithKeyAdded() {
		// call under test
		assertTrue(parsing(List.of("a", "b"), null).isUpsertKeyChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsUpsertKeyChangedFromWithKeyReordered() {
		// call under test
		assertTrue(parsing(List.of("b", "a"), null).isUpsertKeyChangedFrom(parsing(List.of("a", "b"), null)));
	}

	@Test
	public void testIsUpsertKeyChangedFromWithKeySetFromNull() {
		// call under test
		assertTrue(parsing(List.of("a"), null).isUpsertKeyChangedFrom(parsing(null, null)));
	}

	@Test
	public void testIsUpsertKeyChangedFromWithKeyCleared() {
		// call under test
		assertTrue(parsing(null, null).isUpsertKeyChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithNullOnBothSides() {
		// call under test
		assertFalse(parsing(List.of("a"), null).isCsvDescriptorChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithDefaultSetFromNull() {
		// call under test
		assertFalse(parsing(List.of("a"), RecordSetCsvDescriptors.DEFAULT_RECORD_SET_CSV_DESCRIPTOR)
				.isCsvDescriptorChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithEveryDefaultSpelledOut() {
		// call under test
		assertFalse(parsing(List.of("a"), EVERY_DEFAULT_SPELLED_OUT)
				.isCsvDescriptorChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithEmptyDescriptor() {
		// call under test
		assertFalse(parsing(List.of("a"), new CsvTableDescriptor())
				.isCsvDescriptorChangedFrom(parsing(List.of("a"), EVERY_DEFAULT_SPELLED_OUT)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithSeparatorChanged() {
		// call under test
		assertTrue(parsing(List.of("a"), TAB_SEPARATED).isCsvDescriptorChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithNonDefaultClearedToNull() {
		// call under test
		assertTrue(parsing(List.of("a"), null).isCsvDescriptorChangedFrom(parsing(List.of("a"), TAB_SEPARATED)));
	}

	@Test
	public void testIsCsvDescriptorChangedFromWithLineEndChanged() {
		// call under test
		assertTrue(parsing(List.of("a"), new CsvTableDescriptor().setLineEnd("\r\n"))
				.isCsvDescriptorChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsChangedFromWithNoChange() {
		// call under test
		assertFalse(parsing(List.of("a"), EVERY_DEFAULT_SPELLED_OUT).isChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsChangedFromWithUpsertKeyChanged() {
		// call under test
		assertTrue(parsing(List.of("b"), null).isChangedFrom(parsing(List.of("a"), null)));
	}

	@Test
	public void testIsChangedFromWithCsvDescriptorChanged() {
		// call under test
		assertTrue(parsing(List.of("a"), TAB_SEPARATED).isChangedFrom(parsing(List.of("a"), null)));
	}

	private static RecordSetRowParsing parsing(List<String> upsertKey, CsvTableDescriptor csvDescriptor) {
		return new RecordSetRowParsing(upsertKey, csvDescriptor);
	}
}
