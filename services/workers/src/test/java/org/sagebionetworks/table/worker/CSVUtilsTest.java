package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;

public class CSVUtilsTest {

	@Test
	public void isFirstLineHeaderNull() {
		assertTrue(CSVUtils.isFirstRowHeader(null));
	}

	@Test
	public void testIsFirstLineHeaderDescriptor() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(null);
		assertTrue(CSVUtils.isFirstRowHeader(descriptor));
	}

	@Test
	public void testIsFirstLineHeaderDescriptorNotNullTrue() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(Boolean.TRUE);
		assertTrue(CSVUtils.isFirstRowHeader(descriptor));
	}

	@Test
	public void testIsFirstLineHeaderDescriptorNotNullFalse() {
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(Boolean.FALSE);
		assertFalse(CSVUtils.isFirstRowHeader(descriptor));
	}

	@Test
	public void testDoFullScanDefault() {
		assertFalse(CSVUtils.doFullScan(null));
	}
	
	@Test
	public void testDoFullScanNull() {
		UploadToTablePreviewRequest utpr = new UploadToTablePreviewRequest();
		utpr.setDoFullFileScan(null);
		assertFalse(CSVUtils.doFullScan(utpr));
	}
	
	@Test
	public void testDoFullScanProvided() {
		UploadToTablePreviewRequest utpr = new UploadToTablePreviewRequest();
		utpr.setDoFullFileScan(Boolean.TRUE);
		assertTrue(CSVUtils.doFullScan(utpr));
	}

	@Test
	public void testCheckTypeBoolean() {
		String in = "true";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.BOOLEAN, cm.getColumnType());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}

	@Test
	public void testCheckTypeBooleanFalse() {
		ColumnModel cm = CSVUtils.checkType("FALSE", null);
		assertNotNull(cm);
		assertEquals(ColumnType.BOOLEAN, cm.getColumnType());
	}

	@Test
	public void testCheckTypeDoubleNaN() {
		ColumnModel cm = CSVUtils.checkType(Double.toString(Double.NaN), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}

	@Test
	public void testCheckTypeDoubleNagativeInfinity() {
		ColumnModel cm = CSVUtils.checkType(
				Double.toString(Double.NEGATIVE_INFINITY), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}

	@Test
	public void testCheckTypeDoubleOne() {
		ColumnModel cm = CSVUtils.checkType(Double.toString(1.123), null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}

	@Test
	public void testCheckTypeDoubleTwo() {
		ColumnModel cm = CSVUtils.checkType("-1.3e16", null);
		assertNotNull(cm);
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
	}

	@Test
	public void testCheckTypeInteger() {
		ColumnModel cm = CSVUtils.checkType("123", null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}

	@Test
	public void testCheckTypeIntegerNegative() {
		ColumnModel cm = CSVUtils.checkType("-123", null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}

	@Test
	public void testCheckTypeDate() {
		ColumnModel cm = CSVUtils.checkType("2014-05-30 16:29:02.999", null);
		assertNotNull(cm);
		assertEquals(ColumnType.DATE, cm.getColumnType());
	}

	@Test
	public void testCheckTypeEntityId() {
		String in = "syn123";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.ENTITYID, cm.getColumnType());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}

	@Test
	public void testCheckTypeString() {
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}

	@Test
	public void testCheckTypeStringLonger() {
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String longer = in + "more";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(new Long(longer.length()), back.getMaximumSize());
	}

	@Test
	public void testCheckTypeStringShorter() {
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String shorter = in.substring(0, in.length() - 3);
		ColumnModel back = CSVUtils.checkType(shorter, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		// this type the size should not shrink
		assertEquals(new Long(in.length()), back.getMaximumSize());
		assertEquals(cm, back);
	}

	@Test
	public void checkTypeNull() {
		String in = null;
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertEquals(null, cm);
	}

	@Test
	public void checkTypeNullWithExisting() {
		String in = "123";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		// Now if we pass an null value the type should not change
		cm = CSVUtils.checkType(null, cm);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}

	@Test
	public void checkTypeEmpty() {
		String in = "   ";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertEquals(null, cm);
	}

	@Test
	public void checkTypeEmptyWithExisting() {
		String in = "123";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		// Now if we pass an empty value the type should not change
		cm = CSVUtils.checkType("\n", cm);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
	}

	@Test
	public void checkTypeRow() {
		String[] row = new String[] { "string", "123.3", "123", "true",
				"syn123" };
		ColumnModel[] start = new ColumnModel[row.length];
		CSVUtils.checkTypes(row, start);
		// expected
		ColumnType[] expected = new ColumnType[] { ColumnType.STRING,
				ColumnType.DOUBLE, ColumnType.INTEGER, ColumnType.BOOLEAN,
				ColumnType.ENTITYID };
		valiateExpectedTyeps(expected, start);
	}

	@Test
	public void testExtensionGuess() {
		assertEquals("csv", CSVUtils.guessExtension(","));
		assertEquals("tsv", CSVUtils.guessExtension("\t"));
		assertEquals("csv", CSVUtils.guessExtension("!"));
		assertEquals("csv", CSVUtils.guessExtension(null));
	}

	@Test
	public void testContentTypeGuess() {
		assertEquals("text/csv", CSVUtils.guessContentType(","));
		assertEquals("text/tsv", CSVUtils.guessContentType("\t"));
		assertEquals("text/csv", CSVUtils.guessContentType("!"));
		assertEquals("text/csv", CSVUtils.guessContentType(null));
	}

	/**
	 * Validate the expected types.
	 * 
	 * @param expected
	 * @param cm
	 */
	public void valiateExpectedTyeps(ColumnType[] expected, ColumnModel[] cm) {
		assertNotNull(expected);
		assertNotNull(cm);
		assertEquals(expected.length, cm.length);
		for (int i = 0; i < expected.length; i++) {
			assertNotNull(expected[i]);
			assertNotNull(cm[i]);
			assertEquals(expected[i], cm[i].getColumnType());
		}
	}
}
