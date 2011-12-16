package org.sagebionetworks.tool.migration;

/**
 * Keeps track of of basic progress.
 * 
 * @author jmhill
 *
 */
public class BasicProgress {
	
	public static long NANO_SECS_PER_MIL_SEC = 1000000;
	/**
	 * These are volatile because they are updated from one thread and read by another.
	 */
	private volatile long current = 0;
	private volatile long total = 0;
	private volatile long startNano = System.nanoTime();
	
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
	
	/**
	 * Get the total progress as a percentage.
	 * @return
	 */
	public float getFraction(){
		if(current < 1) return 0.0f;
		if(total < 1) return 0.0f;
		float curF = current;
		float totF = total;
		return curF/totF;
	}
	
	/**
	 * Get the current status.
	 * @return
	 */
	public StatusData getCurrentStatus(){
		long nowNano = System.nanoTime();
		long elapseMs = (nowNano-startNano)/NANO_SECS_PER_MIL_SEC;
		float fraction = getFraction();
		long estimatedTotal = Long.MAX_VALUE;
		if(fraction > 0.0f){
			estimatedTotal = (long)(elapseMs/fraction);
		}
		long estimatedRemaingMS = estimatedTotal - elapseMs;
		float percent = fraction*100f;
		return new StatusData(percent, elapseMs, estimatedRemaingMS);
	}
	
	public static class StatusData {
		float percentDone;
		long elaspeTimeMS;
		long estimatedRemaingMS;
		public StatusData(float percentDone, long elaspeTimeMS,
				long estimatedRemaingMS) {
			super();
			this.percentDone = percentDone;
			this.elaspeTimeMS = elaspeTimeMS;
			this.estimatedRemaingMS = estimatedRemaingMS;
		}
		public float getPercentDone() {
			return percentDone;
		}
		public long getElaspeTimeMS() {
			return elaspeTimeMS;
		}
		public long getEstimatedRemaingMS() {
			return estimatedRemaingMS;
		}
		public String toString(){
			return String.format("%1$6.2f %1$2% elapse: %2$tM:%2$tS:%2$tL remaining: ~ %3$tM:%3$tS:%3$tL", percentDone, elaspeTimeMS, estimatedRemaingMS);
		}
	}
	
	public static void main(String[] args) throws InterruptedException{
		System.out.println(String.format("%1$3.2f and %2$2.2f %2$% dd", 123.3334435, 99.34343434));
		System.out.println(String.format("%1$tM:%1$tS:%1$tL ", System.currentTimeMillis()));
		System.out.println(String.format("%1$TM:%1$TS:%1$TL ", 3323l));
		System.out.println(new StatusData(1.34344f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		System.out.println(new StatusData(9.343344f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		System.out.println(new StatusData(87.1f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		
		System.out.println("Starting....");
		BasicProgress p = new BasicProgress();
		p.setTotal(15);
		for(int i=0; i<15; i++){
			p.setCurrent(i);
			System.out.println(p.getCurrentStatus());
			Thread.sleep(833);
		}
	}
	
	

}
