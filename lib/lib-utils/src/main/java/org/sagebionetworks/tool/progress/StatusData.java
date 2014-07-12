package org.sagebionetworks.tool.progress;

/**
 * A class that takes a snapshot of a progress object and prints a meaningful string.
 * @author John
 *
 */
public class StatusData {
	private static final int MS_PER_HOUR = 1000*60*60;
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
	
	/**
	 * Calculate the current status of a progress object.
	 * @param progress
	 */
	public StatusData(Progress progress) {
		super();
		float current = progress.getCurrent();
		float total = progress.getTotal();
		this.elaspeTimeMS = progress.getElapseTimeMS();
		float fraction = 0.0f;
		if(current > 0.0f && total > 0.0f){
			float curF = current;
			float totF = total;
			fraction = curF/totF;
		}
		this.percentDone = fraction*100.0f;
		long estimatedTotal = Long.MAX_VALUE;
		if(fraction > 0.0f){
			estimatedTotal = (long)(elaspeTimeMS/fraction);
		}
		this.estimatedRemaingMS = estimatedTotal - elaspeTimeMS;
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
	
	public String toStringHours(){
		long elapseHours = elaspeTimeMS/MS_PER_HOUR;
		long remainingHours = estimatedRemaingMS/MS_PER_HOUR;
		return String.format("%1$6.2f %1$2% elapse: %4$2d:%2$tM:%2$tS remaining: ~ %5$2d:%3$tM:%3$tS", percentDone, elaspeTimeMS, estimatedRemaingMS, elapseHours, remainingHours);
	}
	
	/**
	 * Test the calculations and output.
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException{
		System.out.println(String.format("Waiting for daemon: %1$s id: %2$d Progress: %3$6.2f %3$2%", "test", 123, 0.34*100f));
		System.out.println(String.format("%1$3.2f and %2$2.2f %2$% dd", 123.3334435, 99.34343434));
		System.out.println(String.format("%1$tM:%1$tS:%1$tL ", System.currentTimeMillis()));
		System.out.println(String.format("%1$TM:%1$TS:%1$TL ", 3323l));
		System.out.println(new StatusData(1.34344f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		System.out.println(new StatusData(9.343344f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		System.out.println(new StatusData(87.1f, (long)(1000*5+123), (long)(1000*6+123)).toString());
		System.out.println("Hours");
		long oneHour = MS_PER_HOUR;
		System.out.println(new StatusData(87.1f, (long)(oneHour*4+12345), (long)(oneHour*5+334000)).toStringHours());
		
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