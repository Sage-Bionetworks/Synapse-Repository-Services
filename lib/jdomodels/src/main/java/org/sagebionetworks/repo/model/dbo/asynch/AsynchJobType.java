package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AsynchUploadJobStatus;

/**
 * This enum maps types to classes.
 * 
 * @author jmhill
 *
 */
public enum AsynchJobType {

	UPLOAD(AsynchUploadJobStatus.class);
	
	
	private Class<? extends AsynchronousJobStatus> clazz;
	
	AsynchJobType(Class<? extends AsynchronousJobStatus> clazz){
		this.clazz = clazz;
	}
	
	/**
	 * Lookup the Type for a given class
	 * @param clazz
	 * @return
	 */
	public static AsynchJobType findType(Class<? extends AsynchronousJobStatus> clazz){
		for(AsynchJobType type: AsynchJobType.values()){
			if(type.clazz.equals(clazz)) return type;
		}
		throw new IllegalArgumentException("Unknown type for class:"+clazz);
	}
	
	/**
	 * The class bound to this type.
	 * @return
	 */
	public Class<? extends AsynchronousJobStatus> getTypeClass(){
		return this.clazz;
	}
}
