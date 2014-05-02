package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.AsynchUploadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchUploadResponseBody;

/**
 * This enum maps types to classes.
 * 
 * @author jmhill
 *
 */
public enum AsynchJobType {
	
	

	UPLOAD(AsynchUploadRequestBody.class, AsynchUploadResponseBody.class,  StackConfiguration.singleton().getTableCSVUploadQueueName());
	
	
	private Class<? extends AsynchronousRequestBody> requestClass;
	private Class<? extends AsynchronousResponseBody> responseClass;
	private String queueName;
	
	AsynchJobType(Class<? extends AsynchronousRequestBody> requestClass, Class<? extends AsynchronousResponseBody> responseClass, String queueName){
		this.requestClass = requestClass;
		this.responseClass = responseClass;
		this.queueName = queueName;
	}
	
	/**
	 * Lookup the Type for a given class
	 * @param clazz
	 * @return
	 */
	public static AsynchJobType findTypeFromRequestClass(Class<? extends AsynchronousRequestBody> clazz){
		for(AsynchJobType type: AsynchJobType.values()){
			if(type.requestClass.equals(clazz)){
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type for class:"+clazz);
	}
	
	/**
	 * The class bound to this type.
	 * @return
	 */
	public Class<? extends AsynchronousRequestBody> getRequestClass(){
		return this.requestClass;
	}
	
	public Class<? extends AsynchronousResponseBody> getResponseClass(){
		return this.responseClass;
	}
	/**
	 * The suffix of the queue name where jobs of this type are published. 
	 * @return
	 */
	public String getQueueName(){
		return this.queueName;
	}
}
