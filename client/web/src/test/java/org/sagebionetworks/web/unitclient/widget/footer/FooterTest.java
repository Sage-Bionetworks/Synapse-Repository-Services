package org.sagebionetworks.web.unitclient.widget.footer;

import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.filter.QueryFilterView;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.footer.FooterView;
import org.sagebionetworks.web.shared.DisplayableValue;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.test.helper.AsyncServiceRecorder;

public class FooterTest {
		
	Footer footer;
	FooterView mockView;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(FooterView.class);		
		footer = new Footer(mockView);
		
		verify(mockView).setPresenter(footer);
	}
	
	@Test
	public void testAsWidget(){
		footer.asWidget();
	}

}
