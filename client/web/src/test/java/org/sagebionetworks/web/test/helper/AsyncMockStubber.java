package org.sagebionetworks.web.test.helper;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncMockStubber {
    public static <T> Stubber callSuccessWith(final T data) {
        return Mockito.doAnswer(new Answer<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T answer(InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] args = invocationOnMock.getArguments();
                ((AsyncCallback<T>) args[args.length - 1]).onSuccess(data);
                return null;
            }
        });
    }

    public static <T extends Throwable> Stubber callFailureWith(final T caught) {
        return Mockito.doAnswer(new Answer<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T answer(InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] args = invocationOnMock.getArguments();
                ((AsyncCallback<T>) args[args.length - 1]).onFailure(caught);
                return null;
            }
        });
    }

}	
