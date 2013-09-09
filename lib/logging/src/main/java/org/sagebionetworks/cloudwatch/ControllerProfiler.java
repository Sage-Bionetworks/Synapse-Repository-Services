package org.sagebionetworks.cloudwatch;

import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tracks latency information and adds it to a Consumer.
 * @author ntiedema
 */
@Aspect
public class ControllerProfiler {
	//constant for nanosecond conversion to milliseconds
	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;

	//a singleton consumer from the Spring settings file 
	@Autowired
	Consumer consumer;
	
	private boolean shouldProfile;
	
	/**
	 * Spring will inject this value.
	 * @param shouldProfile
	 */
	public void setShouldProfile(boolean shouldProfile) {
		this.shouldProfile = shouldProfile;
	}
	
	/**
	 * Default no parameter ControllerProfiler constructor.
	 */
	public ControllerProfiler(){
	}

	/**
	 * One parameter constructor for ControllerProfiler.
	 * @param consumer who receives the latency information
	 */
	public ControllerProfiler(Consumer consumer){
		this.consumer = consumer;
	}	
	
	/**
	 * We want to profile anything that is within a class marked with @Controller
	 * @param ProceedingJoinPoint that holds method invocation information
	 * @return Object that represents method return information
	 */
	@Around("@within(org.springframework.stereotype.Controller)")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
		//do nothing if profiler is not on
		if (!this.shouldProfile){
			//if turned off, just proceed with method
			return pjp.proceed();
		}
		
		//get signature of current thread that is at the ProceedingJoinPoint
		//signature will give us the methodName and information
		Signature signature = pjp.getSignature();
		
		//want the package information for the namespace
		Class declaring = signature.getDeclaringType();
		
		long start = System.nanoTime();	//collect method start time
		Object results = pjp.proceed(); // runs the method
		long end = System.nanoTime();	//collect method end time
		//converting from nanoseconds to milliseconds
		long timeMS = (end - start) /NANOSECOND_PER_MILLISECOND;

		final String metricName = signature.getName() + "-" + StackConfiguration.getStackInstance();
		//use our latency time to make a MetricDatum, and
		//add to synchronized list
		ProfileData profileData = makeProfileDataDTO(declaring.getName(), metricName, timeMS);
		consumer.addProfileData(profileData);
		
		//in configuration file log is set to ERROR to turn off and
		//DEBUG to turn on
//		log.debug("let's see our profileData " + profileData.toString());
		
		//must return whatever method returned
		return results;
	}
	
	/**
	 * Makes transfer object and returns it.
	 * @param String representing namespace/package name
	 * @param String representing method name
	 * @param long representing latency
	 * @return ProfileData Data Transfer Object
	 * @throws IllegalArgumentException
	 */
	public ProfileData makeProfileDataDTO(String namespace, String name, long latency){
		//can't make a ProfileData object if any of the parameters are null
		if (namespace == null || name == null){
			throw (new IllegalArgumentException());
		}
		ProfileData nextPD = new ProfileData();
		nextPD.setNamespace(namespace);
		nextPD.setName(name);
		nextPD.setLatency(latency);
		nextPD.setUnit("Milliseconds");
		nextPD.setTimestamp(new Date());
		
		return nextPD;
		}
	
	/**
	 * Setter for consumer.  
	 * @param consumer
	 */
	protected void setConsumer(Consumer consumer){
		this.consumer = consumer;
	}
	
	/**
	 * Getter for consumer
	 * @return Consumer
	 */
	protected Consumer getConsumer(){
		return this.consumer;
	}
}
