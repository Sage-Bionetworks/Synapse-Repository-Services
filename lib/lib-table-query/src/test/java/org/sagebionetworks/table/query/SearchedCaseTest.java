package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.SearchedCase;

public class SearchedCaseTest {

	@Test
	public void testSearchedCase() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1").searchedCase();
		assertEquals(" WHEN foo > bar THEN 1", element.toSql());
	}
	
	@Test
	public void testSearchedCaseWithElse() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1 else 0").searchedCase();
		assertEquals(" WHEN foo > bar THEN 1 ELSE 0", element.toSql());
	}
	
	@Test
	public void testSearchedCaseWithMultipleWhens() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1 when bar > foo then 2").searchedCase();
		assertEquals(" WHEN foo > bar THEN 1 WHEN bar > foo THEN 2", element.toSql());
	}
	
	@Test
	public void testSearchedCaseWithMultipleWhensAndElse() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1 when bar > foo then 2 else 3").searchedCase();
		assertEquals(" WHEN foo > bar THEN 1 WHEN bar > foo THEN 2 ELSE 3", element.toSql());
	}
}
