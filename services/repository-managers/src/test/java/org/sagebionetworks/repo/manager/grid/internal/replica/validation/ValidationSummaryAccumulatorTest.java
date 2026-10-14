package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class ValidationSummaryAccumulatorTest {

	@Mock
	private CSVWriter mockCsvWriter;

	private ValidationSummaryAccumulator accumulator;

	@BeforeEach
	public void before() throws IOException {
		accumulator = new ValidationSummaryAccumulator(mockCsvWriter);
	}

	@Test
	public void testConstructorWritesHeader() throws IOException {
		// call under test — header is written during construction in @BeforeEach
		verify(mockCsvWriter).writeNext(ValidationSummaryAccumulator.VALIDATION_CSV_HEADERS);
	}

	@Test
	public void testRecordWithNullResult() throws IOException {
		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

		// call under test
		accumulator.record(null);

		verify(mockCsvWriter, times(2)).writeNext(captor.capture());
		String[] dataRow = captor.getAllValues().get(1);
		assertEquals("0", dataRow[0]);   // row_index
		assertNull(dataRow[1]);           // is_valid
		assertNull(dataRow[2]);           // validation_error_message
		assertNull(dataRow[3]);           // all_validation_messages
	}

	@Test
	public void testRecordWithValidResult() throws IOException {
		ValidationResults result = new ValidationResults().setIsValid(true);
		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

		// call under test
		accumulator.record(result);

		verify(mockCsvWriter, times(2)).writeNext(captor.capture());
		String[] dataRow = captor.getAllValues().get(1);
		assertEquals("0", dataRow[0]);
		assertEquals("true", dataRow[1]);
		assertNull(dataRow[2]);
		assertNull(dataRow[3]);
	}

	@Test
	public void testRecordWithInvalidResult() throws IOException {
		List<String> allMessages = List.of("Property 'name' is required", "Value exceeds max length");
		ValidationResults result = new ValidationResults()
				.setIsValid(false)
				.setValidationErrorMessage("Property 'name' is required")
				.setAllValidationMessages(allMessages);
		String expectedAllMessages = JDOSecondaryPropertyUtils.writeStringListToJson(allMessages);

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

		// call under test
		accumulator.record(result);

		verify(mockCsvWriter, times(2)).writeNext(captor.capture());
		String[] dataRow = captor.getAllValues().get(1);
		assertEquals("0", dataRow[0]);
		assertEquals("false", dataRow[1]);
		assertEquals("Property 'name' is required", dataRow[2]);
		assertEquals(expectedAllMessages, dataRow[3]);
	}

	@Test
	public void testRecordWithInvalidResultAndNullMessages() throws IOException {
		ValidationResults result = new ValidationResults()
				.setIsValid(false)
				.setValidationErrorMessage(null)
				.setAllValidationMessages(null);

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

		// call under test — must not throw even when messages are null
		accumulator.record(result);

		verify(mockCsvWriter, times(2)).writeNext(captor.capture());
		String[] dataRow = captor.getAllValues().get(1);
		assertEquals("false", dataRow[1]);
		assertNull(dataRow[2]);
		assertNull(dataRow[3]);
	}

	@Test
	public void testRecordWithNullIsValidCountsAsInvalid() throws IOException {
		// A non-null result with a null isValid field falls through to the invalid branch
		ValidationResults result = new ValidationResults().setIsValid(null);

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

		// call under test
		accumulator.record(result);

		verify(mockCsvWriter, times(2)).writeNext(captor.capture());
		String[] dataRow = captor.getAllValues().get(1);
		assertEquals("false", dataRow[1]);

		ValidationSummaryStatistics summary = accumulator.getValidationSummary("syn1");
		assertEquals(1L, summary.getNumberOfInvalidChildren());
		assertEquals(0L, summary.getNumberOfValidChildren());
		assertEquals(0L, summary.getNumberOfUnknownChildren());
	}

	@Test
	public void testRecordRowIndexIncrements() throws IOException {
		ValidationResults valid = new ValidationResults().setIsValid(true);

		// call under test — three consecutive records
		accumulator.record(valid);
		accumulator.record(valid);
		accumulator.record(valid);

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
		verify(mockCsvWriter, times(4)).writeNext(captor.capture());

		List<String[]> rows = captor.getAllValues();
		assertEquals("0", rows.get(1)[0]);  // first data row
		assertEquals("1", rows.get(2)[0]);  // second data row
		assertEquals("2", rows.get(3)[0]);  // third data row
	}

	@Test
	public void testGetValidationSummaryWithNoRows() {
		// call under test
		ValidationSummaryStatistics summary = accumulator.getValidationSummary("syn123");

		assertEquals("syn123", summary.getContainerId());
		assertEquals(0L, summary.getTotalNumberOfChildren());
		assertEquals(0L, summary.getNumberOfValidChildren());
		assertEquals(0L, summary.getNumberOfInvalidChildren());
		assertEquals(0L, summary.getNumberOfUnknownChildren());
	}

	@Test
	public void testGetValidationSummaryWithMixedRows() {
		ValidationResults valid = new ValidationResults().setIsValid(true);
		ValidationResults invalid = new ValidationResults().setIsValid(false);

		// 2 valid, 2 invalid, 1 unknown (null) in mixed order
		accumulator.record(valid);
		accumulator.record(null);
		accumulator.record(invalid);
		accumulator.record(valid);
		accumulator.record(invalid);

		// call under test
		ValidationSummaryStatistics summary = accumulator.getValidationSummary("syn456");

		assertEquals("syn456", summary.getContainerId());
		assertEquals(5L, summary.getTotalNumberOfChildren());
		assertEquals(2L, summary.getNumberOfValidChildren());
		assertEquals(2L, summary.getNumberOfInvalidChildren());
		assertEquals(1L, summary.getNumberOfUnknownChildren());
	}

	@Test
	public void testGetValidationSummaryContainerId() {
		// call under test
		ValidationSummaryStatistics summary = accumulator.getValidationSummary("syn999");

		assertEquals("syn999", summary.getContainerId());
	}

	@Test
	public void testGetValidationSummaryGeneratedOnIsNotNull() {
		// call under test
		ValidationSummaryStatistics summary = accumulator.getValidationSummary("syn1");

		assertNotNull(summary.getGeneratedOn());
	}

	@Test
	public void testRecordIOExceptionWrappedAsIllegalState() throws IOException {
		IOException cause = new IOException("disk full");
		doThrow(cause).when(mockCsvWriter).writeNext(any(String[].class));

		ValidationResults result = new ValidationResults().setIsValid(true);

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			accumulator.record(result);
		});

		assertEquals("Could not write validation details to CSV file.", ex.getMessage());
		assertEquals(cause, ex.getCause());
	}

}
