package org.sagebionetworks.web.unitclient.widget.breadcrumb;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.breadcrumb.BreadcrumbView;

public class BreadcrumbTest {
		
	Breadcrumb breadcrumb;
	BreadcrumbView mockView;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(BreadcrumbView.class);		
		breadcrumb = new Breadcrumb(mockView);
		
		verify(mockView).setPresenter(breadcrumb);
	}
	
	@Test
	public void testAsWidget(){
		breadcrumb.asWidget();
	}
	
}
