package org.sagebionetworks.web.unitclient.widget.header;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.HeaderView;

public class HeaderTest {
		
	Header header;
	HeaderView mockView;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(HeaderView.class);		
		mockAuthenticationController = Mockito.mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		header = new Header(mockView, mockAuthenticationController, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(header);
	}
	
	@Test
	public void testAsWidget(){
		header.asWidget();
	}
	
}
