package org.sagebionetworks.table.query;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.query.TableQueryModel;

public class TableQueryParserTest {
	
	@Test
	public void testSelectStar() throws ParseException{
		// Parse the query into a basic model object
		TableQueryModel model = TableQueryParser.parserQuery("select *");
		assertNotNull(model);
	}

}
