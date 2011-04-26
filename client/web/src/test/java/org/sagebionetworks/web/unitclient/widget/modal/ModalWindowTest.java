package org.sagebionetworks.web.unitclient.widget.modal;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.modal.ModalWindowView;

public class ModalWindowTest {
		
	ModalWindow modalWindow;
	ModalWindowView mockView;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(ModalWindowView.class);		
		modalWindow = new ModalWindow(mockView);
		
		verify(mockView).setPresenter(modalWindow);
	}
	
	@Test
	public void testAsWidget(){
		modalWindow.asWidget();
	}	
}
