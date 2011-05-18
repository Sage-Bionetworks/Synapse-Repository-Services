package org.sagebionetworks.web.unitclient.widget.footer;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.footer.FooterView;

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
