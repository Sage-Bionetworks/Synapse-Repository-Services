package org.sagebionetworks.web.unitclient.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.view.AbstactHandlerWrapper;
import org.sagebionetworks.web.client.view.HandlerRegistrationProxy;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.RangeChangeEvent.Handler;

public class AbstactHandlerWrapperTest {
	
	// A simple implementation of the abstraction.
	private static class SimpleImpl extends AbstactHandlerWrapper<Handler>{

		public SimpleImpl(HasRows hasRow, Handler handler) {
			super(hasRow, handler);
		}

		@Override
		public HandlerRegistration addHandlerToListner(HasRows hasRow,	Handler handler) {
			return hasRow.addRangeChangeHandler(handler);
		}
	}
	
	HasRows mockHasRows;
	Handler mockHandler;
	HandlerRegistration mockRegistration;
	AbstactHandlerWrapper<Handler> wrapper;
	
	@Before
	public void setup(){
		mockHasRows = Mockito.mock(HasRows.class);
		mockHandler = Mockito.mock(Handler.class);
		mockRegistration = Mockito.mock(HandlerRegistration.class);
		when(mockHasRows.addRangeChangeHandler(mockHandler)).thenReturn(mockRegistration);
		wrapper = new SimpleImpl(mockHasRows, mockHandler);
	}
	
	@Test
	public void testConstructor(){
		assertNotNull(wrapper);
		// The object should already wrap a registration.
		verify(mockHasRows, atLeastOnce()).addRangeChangeHandler(mockHandler);
		assertEquals(mockHandler, wrapper.getHandler());
		assertNotNull(wrapper.getRegistration());
		HandlerRegistrationProxy proxy = wrapper.getRegistration();
		assertEquals(mockRegistration, proxy.getWrapped());
	}
	
	@Test
	public void testRemoveFromOldAndAddToNewListner(){
		assertNotNull(wrapper);
		HasRows newListener = Mockito.mock(HasRows.class);
		HandlerRegistration newReg = Mockito.mock(HandlerRegistration.class);
		when(newListener.addRangeChangeHandler(mockHandler)).thenReturn(newReg);
		// The call
		wrapper.removeFromOldAndAddToNewListner(newListener);
		// The old should get replaced and remove should be called
		verify(mockRegistration, atLeastOnce()).removeHandler();
		
		assertNotNull(wrapper.getRegistration());
		HandlerRegistrationProxy proxy = wrapper.getRegistration();
		assertEquals(newReg, proxy.getWrapped());
		
	}

}
