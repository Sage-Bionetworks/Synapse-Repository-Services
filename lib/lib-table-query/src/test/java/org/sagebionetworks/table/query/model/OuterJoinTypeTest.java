package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

public class OuterJoinTypeTest {
	
	@Test
	public void testAllTypes() throws ParseException {
		for(OuterJoinType type: OuterJoinType.values()) {
			OuterJoinType parsed = new TableQueryParser(type.name()).outerJoinType();
			assertEquals(type, parsed);
		}
	}

}
