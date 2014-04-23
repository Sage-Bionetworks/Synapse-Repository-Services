package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobBody;
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;

/**
 * This enum maps types to classes.
 * 
 * @author jmhill
 *
 */
public enum AsynchJobType {
	
	

	UPLOAD(AsynchUploadJobBody.class, StackConfiguration.singleton().getTableCSVUploadQueueName());
	
	
	private Class<? extends AsynchronousJobBody> clazz;
	private String queueName;
	
	AsynchJobType(Class<? extends AsynchronousJobBody> clazz, String queueName){
		this.clazz = clazz;
		this.queueName = queueName;
	}
	
	/**
	 * Lookup the Type for a given class
	 * @param clazz
	 * @return
	 */
	public static AsynchJobType findType(Class<? extends AsynchronousJobBody> clazz){
		for(AsynchJobType type: AsynchJobType.values()){
			if(type.clazz.equals(clazz)){
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type for class:"+clazz);
	}
	
	/**
	 * The class bound to this type.
	 * @return
	 */
	public Class<? extends AsynchronousJobBody> getTypeClass(){
		return this.clazz;
	}
	
	/**
	 * The suffix of the queue name where jobs of this type are published. 
	 * @return
	 */
	public String getQueueName(){
		return this.queueName;
	}
}
