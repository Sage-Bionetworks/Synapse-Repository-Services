package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

public class RecordSetCsvDescriptorsTest {

	private static final CsvTableDescriptor EVERY_DEFAULT = new CsvTableDescriptor().setSeparator(",")
			.setQuoteCharacter("\"").setEscapeCharacter("\\").setLineEnd("\n").setIsFirstLineHeader(true);

	@Test
	public void testOrDefaultWithNullDescriptor() {
		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.orDefault((CsvTableDescriptor) null);

		assertSame(RecordSetCsvDescriptors.DEFAULT_RECORD_SET_CSV_DESCRIPTOR, result);
	}

	@Test
	public void testOrDefaultWithDescriptor() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor().setSeparator("\t");

		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.orDefault(descriptor);

		assertSame(descriptor, result);
	}

	@Test
	public void testOrDefaultWithRecordSetWithoutDescriptor() {
		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.orDefault(new RecordSet());

		assertSame(RecordSetCsvDescriptors.DEFAULT_RECORD_SET_CSV_DESCRIPTOR, result);
	}

	@Test
	public void testOrDefaultWithRecordSetWithDescriptor() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor().setSeparator("\t");

		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.orDefault(new RecordSet().setCsvDescriptor(descriptor));

		assertSame(descriptor, result);
	}

	@Test
	public void testWithDefaultsWithNullDescriptor() {
		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.withDefaults(null);

		assertEquals(EVERY_DEFAULT, result);
	}

	@Test
	public void testWithDefaultsWithEmptyDescriptor() {
		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.withDefaults(new CsvTableDescriptor());

		assertEquals(EVERY_DEFAULT, result);
	}

	@Test
	public void testWithDefaultsWithEveryPropertySet() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor().setSeparator("\t").setQuoteCharacter("'")
				.setEscapeCharacter("/").setLineEnd("\r\n").setIsFirstLineHeader(false);

		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.withDefaults(descriptor);

		assertEquals(new CsvTableDescriptor().setSeparator("\t").setQuoteCharacter("'").setEscapeCharacter("/")
				.setLineEnd("\r\n").setIsFirstLineHeader(false), result);
	}

	@Test
	public void testWithDefaultsWithSomePropertiesSet() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor().setSeparator("\t");

		// call under test
		CsvTableDescriptor result = RecordSetCsvDescriptors.withDefaults(descriptor);

		assertEquals(new CsvTableDescriptor().setSeparator("\t").setQuoteCharacter("\"").setEscapeCharacter("\\")
				.setLineEnd("\n").setIsFirstLineHeader(true), result);
		// The given descriptor is not modified.
		assertEquals(new CsvTableDescriptor().setSeparator("\t"), descriptor);
	}
}
