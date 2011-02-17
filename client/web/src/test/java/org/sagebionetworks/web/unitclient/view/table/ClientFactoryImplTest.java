package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.view.table.ColumnFactoryGinInjector;
import org.sagebionetworks.web.client.view.table.ColumnFactoryImpl;
import org.sagebionetworks.web.client.view.table.LinkColumn;
import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.ColumnMetadata.RenderType;
import org.sagebionetworks.web.shared.LinkColumnInfo;
import org.sagebionetworks.web.unitclient.presenter.SampleHeaderData;

import com.google.gwt.user.cellview.client.Column;

public class ClientFactoryImplTest {
	
	ColumnFactoryGinInjector mockInjector;
	LinkColumn mockLinkColumn;
	ColumnFactoryImpl factory;
	
	@Before
	public void setup(){
		// Setup the mock injector
		mockInjector = Mockito.mock(ColumnFactoryGinInjector.class);
		mockLinkColumn = Mockito.mock(LinkColumn.class);
		when(mockInjector.getLinkColumn()).thenReturn(mockLinkColumn);
		
		// We are ready to create our factory
		factory = new ColumnFactoryImpl(mockInjector);
	}
	@Test
	public void testBadInput(){
		try{
			factory.createColumn(null);
			fail("An exception should have been thrown");
		}catch (IllegalArgumentException e){
			// expected
		}
	}
	
	@Test
	public void testUnknown(){
		SampleHeaderData fake = new SampleHeaderData("id", "display,", "Desc");
		try{
			// This should fail
			Column<Map<String, Object>, ?> column = factory.createColumn(fake);
			fail("Should have thrown an exception");
		}catch (IllegalArgumentException e){
			// This is the expected behavior
		}
	}
	
	@Test
	public void testLinkColumn(){
		LinkColumnInfo meta = new LinkColumnInfo();
		// We should get a link column from this metadat
		Column<Map<String, Object>, ?> column = factory.createColumn(meta);
		assertNotNull(column);
		// It should be the link column
		assertEquals(mockLinkColumn, column);
		// verify that the column was passed the correct values from the metadata
		verify(mockLinkColumn).setLinkColumnInfo(meta);
	}

}
