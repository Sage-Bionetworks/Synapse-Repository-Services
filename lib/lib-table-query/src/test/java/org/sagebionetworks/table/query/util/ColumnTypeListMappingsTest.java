package org.sagebionetworks.table.query.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnType;

class ColumnTypeListMappingsTest {

	@Test
	public void testForListType(){
		assertEquals(ColumnTypeListMappings.STRING, ColumnTypeListMappings.forListType(ColumnType.STRING_LIST));
		assertEquals(ColumnTypeListMappings.INTEGER, ColumnTypeListMappings.forListType(ColumnType.INTEGER_LIST));
		assertEquals(ColumnTypeListMappings.DATE, ColumnTypeListMappings.forListType(ColumnType.DATE_LIST));
		assertEquals(ColumnTypeListMappings.BOOLEAN, ColumnTypeListMappings.forListType(ColumnType.BOOLEAN_LIST));
		assertEquals(ColumnTypeListMappings.ENTITYID, ColumnTypeListMappings.forListType(ColumnType.ENTITYID_LIST));
		assertEquals(ColumnTypeListMappings.USERID, ColumnTypeListMappings.forListType(ColumnType.USERID_LIST));
	}

	@Test
	public void testForListType_notFound() {
		assertThrows(IllegalArgumentException.class, () -> ColumnTypeListMappings.forListType(ColumnType.STRING));
	}


	@Test
	public void testForNonListType(){
		assertEquals(ColumnTypeListMappings.STRING, ColumnTypeListMappings.forNonListType(ColumnType.STRING));
		assertEquals(ColumnTypeListMappings.INTEGER, ColumnTypeListMappings.forNonListType(ColumnType.INTEGER));
		assertEquals(ColumnTypeListMappings.DATE, ColumnTypeListMappings.forNonListType(ColumnType.DATE));
		assertEquals(ColumnTypeListMappings.BOOLEAN, ColumnTypeListMappings.forNonListType(ColumnType.BOOLEAN));
		assertEquals(ColumnTypeListMappings.ENTITYID, ColumnTypeListMappings.forNonListType(ColumnType.ENTITYID));
		assertEquals(ColumnTypeListMappings.USERID, ColumnTypeListMappings.forNonListType(ColumnType.USERID));
	}

	@Test
	public void testForNonListType_notFound() {
		assertThrows(IllegalArgumentException.class, () -> ColumnTypeListMappings.forNonListType(ColumnType.STRING_LIST));
	}

	@Test
	public void testGetEffectiveMaxCharsPerItemWithExplicitSize() {
		// explicit maximumSize overrides the type default
		assertEquals(42L, ColumnTypeListMappings.STRING.getEffectiveMaxCharsPerItem(42L));
		assertEquals(42L, ColumnTypeListMappings.INTEGER.getEffectiveMaxCharsPerItem(42L));
	}

	@Test
	public void testGetEffectiveMaxCharsPerItemWithNullUsesDefault() {
		// null falls back to each type's built-in default chars-per-element
		assertEquals(ColumnConstants.MAX_ALLOWED_STRING_SIZE, ColumnTypeListMappings.STRING.getEffectiveMaxCharsPerItem(null));
		assertEquals((long) ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING, ColumnTypeListMappings.INTEGER.getEffectiveMaxCharsPerItem(null));
		assertEquals((long) ColumnConstants.MAX_BOOLEAN_CHARACTERS_AS_STRING, ColumnTypeListMappings.BOOLEAN.getEffectiveMaxCharsPerItem(null));
		assertEquals((long) ColumnConstants.MAX_ENTITY_ID_CHARACTERS_AS_STRING, ColumnTypeListMappings.ENTITYID.getEffectiveMaxCharsPerItem(null));
	}

	@ParameterizedTest(name = "{0}: maxCharsPerItem={1}, maxListLength={2}")
	@MethodSource("calculateMaxSizeArguments")
	public void testCalculateMaxSize(ColumnTypeListMappings mapping, Long maxCharsPerItem, Long maxListLength, int expected) {
		// call under test
		assertEquals(expected, mapping.calculateMaxSize(maxCharsPerItem, maxListLength));
	}

	static Stream<Arguments> calculateMaxSizeArguments() {
		int cap = ColumnConstants.MAX_BYTES_PER_LIST_COLUMN_ESTIMATE;
		return Stream.of(
			// STRING (bytesPerChar=4): explicit small values → product 4*10*10=400 is below cap
			Arguments.of(ColumnTypeListMappings.STRING, 10L, 10L, 400),
			// STRING: null maxListLength → derived from budget (100K/1000=100), product 4*1000*100=400K → capped
			Arguments.of(ColumnTypeListMappings.STRING, ColumnConstants.MAX_ALLOWED_STRING_SIZE, null, cap),

			// INTEGER (bytesPerChar=1): product 1*20*20=400 is below cap
			Arguments.of(ColumnTypeListMappings.INTEGER, null, 20L, 400),
			// INTEGER: null maxListLength → derived (100K/20=5000), product 1*20*5000=100K → capped
			Arguments.of(ColumnTypeListMappings.INTEGER, null, null, cap),

			// DATE (bytesPerChar=1): same integer character size as INTEGER
			Arguments.of(ColumnTypeListMappings.DATE, null, 20L, 400),

			// USERID (bytesPerChar=1): same integer character size as INTEGER
			Arguments.of(ColumnTypeListMappings.USERID, null, 20L, 400),

			// BOOLEAN (bytesPerChar=1): product 1*5*52=260 is below cap
			Arguments.of(ColumnTypeListMappings.BOOLEAN, null, 52L, 260),
			// BOOLEAN: null maxListLength → derived (100K/5=20000), product 1*5*20000=100K → capped
			Arguments.of(ColumnTypeListMappings.BOOLEAN, null, null, cap),

			// ENTITYID (bytesPerChar=1): product 1*44*9=396 is below cap
			Arguments.of(ColumnTypeListMappings.ENTITYID, null, 9L, 396),
			// ENTITYID: null maxListLength → derived (100K/44=2272), product 1*44*2272≈100K → capped
			Arguments.of(ColumnTypeListMappings.ENTITYID, null, null, cap)
		);
	}
}