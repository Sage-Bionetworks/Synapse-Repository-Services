package org.sagebionetworks.web.unitclient.widget.statictable;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.modal.ModalWindowView;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableView;

public class StaticTableTest {
		
	StaticTable staticTable;
	StaticTableView mockView;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(StaticTableView.class);		
		staticTable = new StaticTable(mockView);
		
		verify(mockView).setPresenter(staticTable);
	}
	
	@Test
	public void testAsWidget(){
		staticTable.asWidget();
	}	
}
