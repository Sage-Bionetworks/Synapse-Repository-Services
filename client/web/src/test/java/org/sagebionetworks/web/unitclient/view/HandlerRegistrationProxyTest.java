package org.sagebionetworks.web.unitclient.view;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.sagebionetworks.web.client.view.HandlerRegistrationProxy;

import com.google.gwt.event.shared.HandlerRegistration;

public class HandlerRegistrationProxyTest {
	
	HandlerRegistration mockRegistration;
	HandlerRegistrationProxy proxy;
	
	@Before
	public void setup(){
		mockRegistration = Mockito.mock(HandlerRegistration.class);
		proxy = new HandlerRegistrationProxy(mockRegistration);
	}
	
	@Test
	public void testRemoveHandler(){
		// Make sure the remove gets passed to the real object
		proxy.removeHandler();
		verify(mockRegistration, atLeastOnce()).removeHandler();
	}
	
	@Test
	public void testReplaceRegistration(){
		HandlerRegistration newReg = Mockito.mock(HandlerRegistration.class);
		proxy.replaceRegistration(newReg);
		// When we replace, remove should be called on the old.
		verify(mockRegistration, atLeastOnce()).removeHandler();
		
	}

}
