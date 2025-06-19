package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.table.ColumnType;

public class ColumnTypeToConTypeTest {

	private Map<ColumnType, List<String>> possibleValues;

	@BeforeEach
	public void before() {
		possibleValues = new LinkedHashMap<>();
		possibleValues.put(ColumnType.STRING, List.of("", "abc", "\"quoted\""));
		possibleValues.put(ColumnType.DOUBLE, List.of("NaN", "3.14", "-1.0E-12"));
		possibleValues.put(ColumnType.INTEGER, List.of("123", "-101", Long.valueOf(Long.MAX_VALUE).toString(),
				Long.valueOf(Long.MIN_VALUE).toString()));
		possibleValues.put(ColumnType.BOOLEAN, List.of("true", "false"));
		possibleValues.put(ColumnType.DATE, List.of(Long.valueOf(new Date().getTime()).toString()));
		possibleValues.put(ColumnType.FILEHANDLEID, List.of("123"));
		possibleValues.put(ColumnType.ENTITYID, List.of("123"));
		possibleValues.put(ColumnType.SUBMISSIONID, List.of("123"));
		possibleValues.put(ColumnType.EVALUATIONID, List.of("123"));
		possibleValues.put(ColumnType.LINK, List.of("http://foo.org"));
		possibleValues.put(ColumnType.MEDIUMTEXT, List.of("big"));
		possibleValues.put(ColumnType.LARGETEXT, List.of("biger"));
		possibleValues.put(ColumnType.USERID, List.of("123"));
		possibleValues.put(ColumnType.STRING_LIST, List.of("[]", "[\"a\",\"b\"]"));
		possibleValues.put(ColumnType.INTEGER_LIST, List.of("[]", "[1,2,3]"));
		possibleValues.put(ColumnType.BOOLEAN_LIST, List.of("[]", "[true,false]"));
		possibleValues.put(ColumnType.DATE_LIST, List.of("[]", "[1,2,3]"));
		possibleValues.put(ColumnType.ENTITYID_LIST, List.of("[]", "[1,2,3]"));
		possibleValues.put(ColumnType.USERID_LIST, List.of("[]", "[1,2,3]"));
		possibleValues.put(ColumnType.JSON, List.of("[]", "{}", "[1,2,3]", "{\"a\":true}"));
	}

	@ParameterizedTest
	@EnumSource(ColumnType.class)
	public void testEachType(ColumnType type) {

		// Call under test
		ColumnTypeToConType cttc = ColumnTypeToConType.lookUpType(type);

		ConValue nullCon = cttc.getTranslator().translateNullable(null);
		assertEquals(new ConValue(ConType.NULL, null), nullCon);
		possibleValues.get(type).forEach(s -> {
			ConValue value = cttc.getTranslator().translateNullable(s);
			assertNotNull(value);
			assertEquals(value.getValue().toString(), s);
		});
	}

}
