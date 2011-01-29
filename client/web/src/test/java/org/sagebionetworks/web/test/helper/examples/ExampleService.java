package org.sagebionetworks.web.test.helper.examples;
import java.io.IOException;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This example service is has many of the characteristics that will occur
 * in real services.
 * @author John
 *
 */
@RemoteServiceRelativePath("message")
public interface ExampleService extends RemoteService {
	
	/**
	 * No argument return collection
	 * @return
	 */
	List<SampleDTO> noArgs();
	
	/**
	 * Collection in and out.
	 * @param idList
	 * @return
	 */
	List<SampleDTO> withArgs(List<Integer> idList);
	
	/**
	 * Return null
	 * @return
	 */
	List<SampleDTO> nullReturn();
	
	/**
	 * Two methods with the same name but 
	 * @param name
	 * @return
	 */
	SampleDTO getSampleOverload(String name);
	
	SampleDTO getSampleOverload(int id);
	
	SampleDTO getSampleOverload(Exception e);
	
	SampleDTO getSampleOverload(IOException e);
	
	void voidReturn(int id);
	
	boolean allPrimitives(long count, int id);
	
	int createSample(String name, String description);
	
	boolean deleteSample(int id);
	
	boolean throwsException(String message) throws IOException;
	

}
