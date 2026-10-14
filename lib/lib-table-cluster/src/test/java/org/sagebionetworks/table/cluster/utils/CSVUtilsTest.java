package org.sagebionetworks.table.cluster.utils;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.Constants;

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
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
		// Should yield the same type
		ColumnModel back = CSVUtils.checkType(in, cm);
		assertEquals(cm, back);
	}
	
	@Test
	public void testCheckTypeStringUnderLimit(){
		String stringUnderLimit = createStringOfSize(ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue());
		ColumnModel cm = CSVUtils.checkType(stringUnderLimit, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(Long.valueOf(stringUnderLimit.length()), cm.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeMediumText(){
		String stringOverLimit = createStringOfSize(ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue()+1);
		ColumnModel cm = CSVUtils.checkType(stringOverLimit, null);
		assertNotNull(cm);
		assertEquals(ColumnType.MEDIUMTEXT, cm.getColumnType());
		assertEquals(Long.valueOf(stringOverLimit.length()), cm.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeLargeText(){
		String stringOverLimit = createStringOfSize((int)ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS+1);
		ColumnModel cm = CSVUtils.checkType(stringOverLimit, null);
		assertNotNull(cm);
		assertEquals(ColumnType.LARGETEXT, cm.getColumnType());
		assertEquals(Long.valueOf(stringOverLimit.length()), cm.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeLargeTextOverLimit(){
		String stringOverLimit = createStringOfSize((int)ColumnConstants.MAX_LARGE_TEXT_CHARACTERS+1);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			CSVUtils.checkType(stringOverLimit, null);
		}).getMessage();
		assertEquals(CSVUtils.ERROR_CELLS_EXCEED_MAX, message);
	}
	
	/**
	 * Helper to create a string of the given size.
	 * @param size
	 * @return
	 */
	public static String createStringOfSize(int size){
		char[] chars = new char[size];
		Arrays.fill(chars, 'a');
		return new String(chars);
	}

	@Test
	public void testCheckTypeStringLonger() {
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String longer = in + "more";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(Long.valueOf(longer.length()), back.getMaximumSize());
	}

	@Test
	public void testCheckTypeStringShorter() {
		String in = "a simple string";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
		// If we give it a longer string then it should grow.
		String shorter = in.substring(0, in.length() - 3);
		ColumnModel back = CSVUtils.checkType(shorter, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		// this type the size should not shrink
		assertEquals(Long.valueOf(in.length()), back.getMaximumSize());
		assertEquals(cm, back);
	}
	
	/**
	 * If the current is a short string, the type must remain a string but longer.
	 */
	@Test
	public void testCheckTypeShortStringLong() {
		String in = "X";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
		// Given an integer that is longer
		String longer = "15";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(Long.valueOf(2), back.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeLongIntegerToString() {
		String in = "123456789";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		// Given an integer that is longer
		String longer = "X";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
	}
	
	@Test
	public void testCheckTypeBooleanToShortString() {
		String in = "true";
		ColumnModel cm = CSVUtils.checkType(in, null);
		assertNotNull(cm);
		assertEquals(ColumnType.BOOLEAN, cm.getColumnType());
		// Given an integer that is longer
		String longer = "X";
		ColumnModel back = CSVUtils.checkType(longer, cm);
		assertEquals(ColumnType.STRING, back.getColumnType());
		assertEquals(Long.valueOf(in.length()), cm.getMaximumSize());
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
	
	@Test
	public void testCreateCSVReaderAllDefaults(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(Constants.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvReader.getQuoteChar());
		assertEquals(0, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderTabSeperator(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setSeparator("\t");
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals('\t', csvReader.getSeparator());
		assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvReader.getQuoteChar());
		assertEquals(0, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderEscapse(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setEscapeCharacter("\n");
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(Constants.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals('\n', csvReader.getEscape());
		assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvReader.getQuoteChar());
		assertEquals(0, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderQuote(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setQuoteCharacter("'");
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(Constants.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals('\'', csvReader.getQuoteChar());
		assertEquals(0, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderSkipLine(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setLinesToSkip(101L);
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(Constants.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvReader.getQuoteChar());
		assertEquals(101, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderAllOverride(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setSeparator("-");
		body.getCsvTableDescriptor().setEscapeCharacter("?");
		body.getCsvTableDescriptor().setQuoteCharacter(":");
		body.setLinesToSkip(12L);
		StringReader reader = new StringReader("1,2,3");
		CSVReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals('-', csvReader.getSeparator());
		assertEquals('?', csvReader.getEscape());
		assertEquals(':', csvReader.getQuoteChar());
		assertEquals(12, csvReader.getSkipLines());
	}

	@Test
	public void testCheckTypeJsonObject() {
		ColumnModel cm = CSVUtils.checkType("{\"a\":1}", null);
		assertNotNull(cm);
		assertEquals(ColumnType.JSON, cm.getColumnType());
	}

	@Test
	public void testCheckTypeEmptyJsonObject() {
		ColumnModel cm = CSVUtils.checkType("{}", null);
		assertNotNull(cm);
		assertEquals(ColumnType.JSON, cm.getColumnType());
	}

	@Test
	public void testCheckTypeIntegerArray() {
		ColumnModel cm = CSVUtils.checkType("[1,2,3]", null);
		assertNotNull(cm);
		assertEquals(ColumnType.INTEGER_LIST, cm.getColumnType());
	}

	@Test
	public void testCheckTypeStringArray() {
		ColumnModel cm = CSVUtils.checkType("[\"a\",\"b\"]", null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
	}

	@Test
	public void testCheckTypeEmptyArray() {
		// Empty arrays are treated as no data
		ColumnModel cm = CSVUtils.checkType("[]", null);
		assertEquals(null, cm);
	}

	@Test
	public void testCheckTypeEmptyArrayWithExistingType() {
		ColumnModel existing = new ColumnModel();
		existing.setColumnType(ColumnType.INTEGER_LIST);
		existing.setMaximumSize(5L);
		existing.setMaximumListLength(2L);
		ColumnModel cm = CSVUtils.checkType("[]", existing);
		// Should not change the existing type
		assertEquals(existing, cm);
	}

	@Test
	public void testCheckTypeNestedArray() {
		ColumnModel cm = CSVUtils.checkType("[[1,2],[3,4]]", null);
		assertNotNull(cm);
		// This is a side effect of the JSONArray reading this as an array of strings.
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
	}

	@Test
	public void testCheckTypeArrayWithObjects() {
		ColumnModel cm = CSVUtils.checkType("[{\"a\":1}]", null);
		assertNotNull(cm);
		// This is a side effect of the JSONArray reading this as an array of strings.
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
	}

	@Test
	public void testCheckTypeArrayWithNulls() {
		ColumnModel cm = CSVUtils.checkType("[null, 1]", null);
		assertNotNull(cm);
		assertEquals(ColumnType.JSON, cm.getColumnType());
	}

	@Test
	public void testCheckTypeMixedArray() {
		// Mixed integers and strings -> STRING_LIST
		ColumnModel cm = CSVUtils.checkType("[1, \"a\"]", null);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
	}

	@Test
	public void testCheckTypeWidenIntegerListToStringList() {
		ColumnModel intList = new ColumnModel();
		intList.setColumnType(ColumnType.INTEGER_LIST);
		intList.setMaximumSize(5L);
		intList.setMaximumListLength(3L);
		ColumnModel cm = CSVUtils.checkType("[\"a\",\"b\"]", intList);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(ColumnConstants.DEFAULT_LIST_LENGTH, cm.getMaximumListLength());
	}

	@Test
	public void testCheckTypeWidenIntegerListToJson() {
		ColumnModel intList = new ColumnModel();
		intList.setColumnType(ColumnType.INTEGER_LIST);
		intList.setMaximumSize(5L);
		intList.setMaximumListLength(3L);
		ColumnModel cm = CSVUtils.checkType("{\"a\":1}", intList);
		assertNotNull(cm);
		assertEquals(ColumnType.JSON, cm.getColumnType());
	}

	@Test
	public void testCheckTypeWidenStringListToJson() {
		ColumnModel strList = new ColumnModel();
		strList.setColumnType(ColumnType.STRING_LIST);
		strList.setMaximumSize(5L);
		strList.setMaximumListLength(2L);
		ColumnModel cm = CSVUtils.checkType("{\"a\":1}", strList);
		assertNotNull(cm);
		assertEquals(ColumnType.JSON, cm.getColumnType());
	}

	@Test
	public void testCheckTypeJsonToNonJsonFallsToString() {
		ColumnModel jsonType = new ColumnModel();
		jsonType.setColumnType(ColumnType.JSON);
		jsonType.setMaximumSize(5L);
		ColumnModel cm = CSVUtils.checkType("abc", jsonType);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
	}

	@Test
	public void testCheckTypeNonJsonToJsonFallsToString() {
		ColumnModel intType = new ColumnModel();
		intType.setColumnType(ColumnType.INTEGER);
		intType.setMaximumSize(3L);
		ColumnModel cm = CSVUtils.checkType("[1,2,3]", intType);
		assertNotNull(cm);
		assertEquals(ColumnType.STRING, cm.getColumnType());
	}
	
	@Test
	public void testCheckTypeWithStartBracketOnly() {
		// call under test
		ColumnModel cm = CSVUtils.checkType("[one json", null);
		assertEquals(new ColumnModel().setColumnType(ColumnType.STRING).setMaximumSize(9L), cm);
	}
	
	@Test
	public void testCheckTypeWithStartCurlyBracketOnly() {
		// call under test
		ColumnModel cm = CSVUtils.checkType("{one json", null);
		assertEquals(new ColumnModel().setColumnType(ColumnType.STRING).setMaximumSize(9L), cm);
	}
	
	@Test
	public void testCheckTypeWithCurrentJSONAndValueJSON() {
		ColumnModel current = new ColumnModel().setColumnType(ColumnType.JSON).setMaximumSize(4L);
		// call under test
		ColumnModel cm = CSVUtils.checkType("{\"a\":123}",current);
		assertEquals(new ColumnModel().setColumnType(ColumnType.JSON).setMaximumSize(9L), cm);
	}

	@Test
	public void testCheckTypeWithCurrentStringListValueStringList() {
		ColumnModel current = new ColumnModel().setColumnType(ColumnType.STRING_LIST).setMaximumSize(4L)
				.setMaximumListLength(1L);
		// call under test
		ColumnModel cm = CSVUtils.checkType("[\"a\",\"b\"]", current);
		// maximumSize is max of current (4) and longest element "a"/"b" (1) = 4
		assertEquals(
				new ColumnModel().setColumnType(ColumnType.STRING_LIST).setMaximumSize(4L)
						.setMaximumListLength(ColumnConstants.DEFAULT_LIST_LENGTH),
				cm);
	}

	@Test
	public void testCheckTypeWithCurrentIntListAndValueIntList() {
		ColumnModel current = new ColumnModel().setColumnType(ColumnType.INTEGER_LIST).setMaximumSize(4L);
		// call under test
		ColumnModel cm = CSVUtils.checkType("[1,2,3,4]", current);
		assertEquals(
				new ColumnModel().setColumnType(ColumnType.INTEGER_LIST).setMaximumSize(9L)
						.setMaximumListLength(ColumnConstants.DEFAULT_LIST_LENGTH),
				cm);
	}

	@Test
	public void testParseListValueWithValidArray() {
		Optional<CSVUtils.ListInfo> result = CSVUtils.parseListValue("[\"a\",\"bb\",\"ccc\"]");
		assertTrue(result.isPresent());
		assertEquals(3, result.get().listSize);
		assertEquals(3, result.get().maxElementSize);
	}

	@Test
	public void testParseListValueWithEmptyArray() {
		Optional<CSVUtils.ListInfo> result = CSVUtils.parseListValue("[]");
		assertTrue(result.isPresent());
		assertEquals(0, result.get().listSize);
		assertEquals(0, result.get().maxElementSize);
	}

	@Test
	public void testParseListValueWithNotAnArray() {
		assertFalse(CSVUtils.parseListValue("not json").isPresent());
	}

	@Test
	public void testParseListValueWithJsonObject() {
		assertFalse(CSVUtils.parseListValue("{\"a\":1}").isPresent());
	}

	@Test
	public void testParseListValueWithInvalidBracketString() {
		assertFalse(CSVUtils.parseListValue("[not json").isPresent());
	}

	@Test
	public void testParseListValueWithIntegerArray() {
		Optional<CSVUtils.ListInfo> result = CSVUtils.parseListValue("[1,2,3]");
		assertTrue(result.isPresent());
		assertEquals(3, result.get().listSize);
		assertEquals(1, result.get().maxElementSize);
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
