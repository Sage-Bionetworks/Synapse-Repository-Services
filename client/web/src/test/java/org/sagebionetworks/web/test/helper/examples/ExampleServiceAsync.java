package org.sagebionetworks.web.test.helper.examples;

import java.io.IOException;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ExampleServiceAsync {

	void noArgs(AsyncCallback<List<SampleDTO>> callback);

	void getSampleOverload(String name, AsyncCallback<SampleDTO> callback);

	void getSampleOverload(int id, AsyncCallback<SampleDTO> callback);

	void voidReturn(int id, AsyncCallback<Void> callback);

	void allPrimitives(long count, int id, AsyncCallback<Boolean> callback);

	void nullReturn(AsyncCallback<List<SampleDTO>> callback);

	void createSample(String name, String description, AsyncCallback<Integer> callback);

	void withArgs(List<Integer> idList, AsyncCallback<List<SampleDTO>> callback);

	void deleteSample(int id, AsyncCallback<Boolean> callback);

	void getSampleOverload(Exception e, AsyncCallback<SampleDTO> callback);

	void getSampleOverload(IOException e, AsyncCallback<SampleDTO> callback);

	void throwsException(String message, AsyncCallback<Boolean> callback);

	
}
