package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

public class RecordSetCsvDescriptorsTest {

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
}
