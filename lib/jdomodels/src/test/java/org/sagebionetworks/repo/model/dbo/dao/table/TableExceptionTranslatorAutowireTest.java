package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableExceptionTranslatorAutowireTest {
	
	@Autowired
	private ColumnModelDAO columnModelDao;
	
	@Autowired
	private TableExceptionTranslator translator;
	
	@Test
	public void testTranslate() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(50L);
		cm.setName("foo");
		cm = columnModelDao.createColumnModel(cm);
		
		SQLException exception = new SQLException("Cannot find column: '_C"+cm.getId()+"_'");
		RuntimeException translated = translator.translateException(exception);
		assertNotNull(translated);
		assertEquals("Cannot find column: 'foo'", translated.getMessage());
	}

}
