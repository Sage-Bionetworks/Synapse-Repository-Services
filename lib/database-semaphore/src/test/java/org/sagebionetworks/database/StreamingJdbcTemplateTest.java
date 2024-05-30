package org.sagebionetworks.database;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class StreamingJdbcTemplateTest {
	
	@Mock
	private DataSource mockDatasource;
	@Mock
	private Statement mockStatement;

	
	@Test
	public void testApplyStatementSettings() throws SQLException{
		StreamingJdbcTemplate template = new StreamingJdbcTemplate(mockDatasource);
		assertEquals(Integer.MIN_VALUE, template.getFetchSize());
		// the Integer.MIN_VALUE  must be applied to a statement
		template.applyStatementSettings(mockStatement);
		verify(mockStatement).setFetchSize(Integer.MIN_VALUE);
	}

}
