package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.view.table.ColumnFactoryGinInjector;
import org.sagebionetworks.web.client.view.table.ColumnFactoryImpl;
import org.sagebionetworks.web.client.view.table.DateColumn;
import org.sagebionetworks.web.client.view.table.LayerColumn;
import org.sagebionetworks.web.client.view.table.LayerTypeColumn;
import org.sagebionetworks.web.client.view.table.LinkColumn;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.DateColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.LayerColumnInfo;
import org.sagebionetworks.web.shared.LinkColumnInfo;
import org.sagebionetworks.web.unitclient.presenter.SampleHeaderData;
import org.sagebionetworks.web.util.ServerPropertiesUtils;

import com.google.gwt.user.cellview.client.Column;

public class ClientFactoryImplTest {

	ColumnFactoryGinInjector mockInjector;
	LinkColumn mockLinkColumn;
	LayerColumn mockLayerColumn;
	DateColumn mockDateColumn;
	LayerTypeColumn mockLayerType;
	ColumnFactoryImpl factory;

	private static ColumnConfigProvider serverProvider;
	
	@BeforeClass
	public static void beforeClass() throws IOException{
		// Load the runtime column configuration to tests that all column types are supported.
		Properties props = ServerPropertiesUtils.loadProperties();
		String columnConfigFile = props.getProperty(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE);
		// Create the column config from the classpath
		serverProvider = new ColumnConfigProvider(columnConfigFile);
	}

	@Before
	public void setup() {
		// Setup the mock injector		
		mockInjector = new ColumnFactoryGinInjector() {
			
			@Override
			public LinkColumn getLinkColumn() {
				mockLinkColumn = Mockito.mock(LinkColumn.class);
				return mockLinkColumn;
			}
			
			@Override
			public LayerTypeColumn getLayerTypeColumn() {
				mockLayerType = Mockito.mock(LayerTypeColumn.class);
				return mockLayerType;
			}
			
			@Override
			public LayerColumn getLayerColumn() {
				mockLayerColumn = Mockito.mock(LayerColumn.class);
				return mockLayerColumn;
			}
			
			@Override
			public DateColumn getDateColumn() {
				mockDateColumn = Mockito.mock(DateColumn.class);
				return mockDateColumn;
			}
		};
		
		// We are ready to create our factory
		factory = new ColumnFactoryImpl(mockInjector);
	}

	@Test
	public void testBadInput() {
		try {
			factory.createColumn(null);
			fail("An exception should have been thrown");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testUnknown() {
		SampleHeaderData fake = new SampleHeaderData("id", "display,", "Desc");
		try {
			// This should fail
			Column<Map<String, Object>, ?> column = factory.createColumn(fake);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			// This is the expected behavior
		}
	}

	@Test
	public void testLinkColumn() {
		LinkColumnInfo meta = new LinkColumnInfo();
		// We should get a link column from this metadat
		Column<Map<String, Object>, ?> column = factory.createColumn(meta);
		assertNotNull(column);
		// It should be the link column
		assertEquals(mockLinkColumn, column);
		// verify that the column was passed the correct values from the
		// metadata
		verify(mockLinkColumn).setLinkColumnInfo(meta);
	}
	
	@Test
	public void testLayerColumnColumn() {
		LayerColumnInfo meta = new LayerColumnInfo();
		// We should get a link column from this metadat
		Column<Map<String, Object>, ?> column = factory.createColumn(meta);
		assertNotNull(column);
		// It should be the layer column
		assertEquals(mockLayerColumn, column);
		// verify that the column was passed the correct values from the
		// metadata
		verify(mockLayerColumn).setLayerColumnInfo(meta);
	}
	
	@Test
	public void testDateColumn() {
		DateColumnInfo meta = new DateColumnInfo();
		// We should get a link column from this metadat
		Column<Map<String, Object>, ?> column = factory.createColumn(meta);
		assertNotNull(column);
		// It should be the layer column
		assertEquals(mockDateColumn, column);
		// verify that the column was passed the correct values from the
		// metadata
		verify(mockDateColumn).setDateColumnInfo(meta);
	}

	@Test
	public void testAllTypes() {
		List<HeaderData> allColumnInfoTypes = createEachTypeHeader();
		assertNotNull(allColumnInfoTypes);
		// Try each type
		for (int i = 0; i < allColumnInfoTypes.size(); i++) {
			HeaderData meta = allColumnInfoTypes.get(i);
			Column<Map<String, Object>, ?> column = factory.createColumn(meta);
			assertNotNull(column);
		}
	}
	
	@Test
	public void testAllRuntimeTypes(){
		// Make sure we can support all of the runtime types
		Iterator<String> keyIt = serverProvider.getKeyIterator();
		while(keyIt.hasNext()){
			String key = keyIt.next();
			HeaderData header = serverProvider.get(key);
			assertNotNull(header); 
			Column<Map<String, Object>, ?> column = factory.createColumn(header);
			assertNotNull(column);
		}
	}

	/**
	 * Helper to create on of each type of column.
	 * 
	 * @return
	 */
	public static List<HeaderData> createEachTypeHeader() {
		List<HeaderData> list = new ArrayList<HeaderData>();
		ColumnInfo.Type[] types = ColumnInfo.Type.values();
		for (int i = 0; i < types.length; i++) {
			list.add(new ColumnInfo("id" + i, types[i].name(), "display" + i,
					"desc" + i));
		}
		return list;
	}
}
