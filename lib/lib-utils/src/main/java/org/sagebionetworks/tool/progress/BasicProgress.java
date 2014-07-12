package org.sagebionetworks.tool.progress;


/**
 * Keeps track of of basic progress.
 * 
 * @author jmhill
 *
 */
public class BasicProgress implements Progress{
	
	public static long NANO_SECS_PER_MIL_SEC = 1000000;

	/**
	 * These are volatile because they are updated from one thread and read by another.
	 */
	private volatile long current = 0;
	private volatile long total = 0;
	private volatile long startNano = System.nanoTime();
	private volatile long elapse = 0;
	private volatile boolean done = false;
	private volatile String message;
	
	public long getCurrent() {
		return current;
	}
	public void setCurrent(long current) {
		this.current = current;
	}
	public long getTotal() {
		return total;
	}
	public void setTotal(long total) {
		this.total = total;
	}
	public void setDone(){
		done = true;
		current = total;
		elapse = calulcateElapse();
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public long getElapseTimeMS() {
		if(!done){
			return (System.nanoTime()-startNano)/NANO_SECS_PER_MIL_SEC;
		}else{
			return elapse;
		}
	}
	
	private long calulcateElapse(){
		return (System.nanoTime()-startNano)/NANO_SECS_PER_MIL_SEC;
	}
	@Override
	public StatusData getCurrentStatus() {
		return new StatusData(this);
	}

}
