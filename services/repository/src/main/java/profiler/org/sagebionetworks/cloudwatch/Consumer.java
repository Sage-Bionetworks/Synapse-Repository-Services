package profiler.org.sagebionetworks.cloudwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

/**
 * Sends latency information to AmazonWebServices CloudWatch. It's the consumer
 * in the producer/consumer pattern and it handles the Watchers in the Observer
 * pattern. Watchers can monitor success or failure of "puts" to CloudWatch
 * 
 * @author ntiedema
 */
public class Consumer implements MetricDatumSubject {
	static private Log log = LogFactory.getLog(Consumer.class);

	// synchronized list to hold MetricDatums objects
	private List<ProfileData> listProfileData = Collections
			.synchronizedList(new ArrayList<ProfileData>());

	// list of watchers who will be notified with success/failure of all "puts"
	List<Watcher> listOfWatchers = Collections
			.synchronizedList(new ArrayList<Watcher>());

	// need a cloudWatch client
	@Autowired
	AmazonCloudWatchClient cloudWatchClient;

	/**
	 * No parameter consumer constructor.
	 */
	public Consumer() {
	}

	/**
	 * Consumer constructor that takes AmazonCloudWatch client as parameter.
	 * @param client for Amazon
	 */
	public Consumer(AmazonCloudWatchClient cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}

	/**
	 * Takes a ProfileData and adds it to synchronized list. If ProfileData item
	 * is null, it does not add to the list.
	 * 
	 * @param addToListMDS
	 *            ProfileData Data Transfer Object
	 * @throws IllegalArgumentException
	 *             if the given object is null
	 */
	public void addProfileData(ProfileData addToList) {
		// add method will allow null items
		if (addToList == null) {
			throw (new IllegalArgumentException());
		}
		listProfileData.add(addToList);
	}

	/**
	 * removes ProfileData from synchronized list and sends to CloudWatch.
	 * 
	 * @return List<String> where each string represents "put" success/failure
	 */
	public List<String> executeCloudWatchPut() {
		try {
			// collect the ProfileData from synchronized list
			List<ProfileData> nextBunch = new ArrayList<ProfileData>();
			synchronized (listProfileData) {
				nextBunch.addAll(listProfileData);
				listProfileData.clear();
				}

			//here I have a list of potentially different namespaces
			//convert to a map (key is namespace, value is list of metricDatums)
			Map<String, List<MetricDatum>> allTheNamespaces = getAllNamespaces(nextBunch);
			//need to collect the messages for testing
			List<String> toReturn = new ArrayList<String>();
		
			//need to do a put to cloudWatch for each namespace 
			//if the map has no items this loop will end immediately
		
			//iterate through map
			for (String key : allTheNamespaces.keySet()){
				PutMetricDataRequest toSendToCW = new PutMetricDataRequest();
				toSendToCW.setNamespace(key);
				toSendToCW.setMetricData(allTheNamespaces.get(key));
				toReturn.addAll(sendBatches(toSendToCW));
			}
			//this will have a message for each batch sent to CloudWatch
			//if no batches were sent to CloudWatch this will have size of 0
			return toReturn;
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * Returns a map of namespaces, with value being list of each MetricDatum
	 * parameter list contained for that namespace.
	 * 
	 * @param list
	 *            <ProfileData>
	 * @return Map<String, List<MetricDatum>>
	 */
	public Map<String, List<MetricDatum>> getAllNamespaces(
			List<ProfileData> list) {
		// need return map
		Map<String, List<MetricDatum>> toReturn = new HashMap<String, List<MetricDatum>>();

		// loop through the list
		for (ProfileData pd : list) {
			if (!toReturn.containsKey(pd.getNamespace())) {
				// if the list to return does not have the current
				// key/namespace in it
				// need to add the namespace and a list of MetricDatums
				List<MetricDatum> listMD = new ArrayList<MetricDatum>();
				// add to list the current ProfileData in the form of a
				// metricDatum
				listMD.add(makeMetricDatum(pd));
				toReturn.put(pd.getNamespace(), listMD);
			} else {
				// here list already had namespace, but needs to add
				// new metricDatum
				List<MetricDatum> listMD = toReturn.get(pd.getNamespace());
				listMD.add(makeMetricDatum(pd));
				toReturn.put(pd.getNamespace(), listMD);
			}
		}
		return toReturn;
	}

	/**
	 * Converts a ProfileData to a MetricDatum.
	 * 
	 * @param ProfileData
	 * @return MetricDatum throws IllegalArgumentException if parameter object
	 *         is null
	 */
	public MetricDatum makeMetricDatum(ProfileData pd) {
		if (pd == null) {
			throw (new IllegalArgumentException());
		}
		
		//AmazonWebServices requires the MetricDatum have a namespace
		//and unit can't be smaller than zero as it represents latency
		if (pd.getName() == null || pd.getName() == "" || pd.getLatency() < 0){
			throw (new IllegalArgumentException());
		}
		MetricDatum toReturn = new MetricDatum();
		toReturn.setMetricName(pd.getName());
		toReturn.setValue((double) pd.getLatency());
		toReturn.setUnit(pd.getUnit());
		toReturn.setTimestamp(pd.getTimestamp());
		return toReturn;
	}

	/**
	 * Sends MetricDatum items to CloudWatch in batches of 20 or less. Amazon
	 * Web Services will not allow a "put" of over 20 items, so the
	 * PutMetricDataRequest item must be divided into chunks.
	 * 
	 * @param pmdr
	 *            that represents a PutMetricDataRequest object that contains a
	 *            list of MetricDataum objects whose size could be over 20.
	 * @return is a list of strings that represent a success/failure message for
	 *         each individual "put" to CloudWatch.
	 * @throws IllegalArgumentException
	 *             is parameter is null
	 */
	public List<String> sendBatches(PutMetricDataRequest pmdr) {
		if (pmdr == null) {
			throw (new IllegalArgumentException());
		}

		// need a list to hold the success/failure message for each "put"
		List<String> toReturn = new ArrayList<String>();

		// need a list to hold all the MetricDatum objects in parameter
		// items will be removed from this list as they are sent to CloudWatch
		List<MetricDatum> allTheMetricDatums = pmdr.getMetricData();

		// if the list is empty, then will not enter the list and size
		// of toReturn will be 0

		// Put will send an error if it holds a list of > 20 objects
		while (allTheMetricDatums.size() > 0) {
			if (allTheMetricDatums.size() > 20) {
				// need a list to hold the next 20
				List<MetricDatum> next20MetricDatums = new ArrayList<MetricDatum>();
				for (int i = 0; i < 20; i++) {
					// add the last item on the list and make sure it is
					// removed from the original list
					next20MetricDatums.add(allTheMetricDatums
							.remove(allTheMetricDatums.size() - 1));
				}
				// need a PutMetricDataRequest object for each group of 20
				PutMetricDataRequest next20 = new PutMetricDataRequest();
				next20.setNamespace(pmdr.getNamespace());
				next20.setMetricData(next20MetricDatums);
				// now put object is ready to send to CloudWatch
				String nextReturnMessage = sendMetrics(next20, cloudWatchClient);
				toReturn.add(nextReturnMessage);
			} else {
				PutMetricDataRequest lastPut = new PutMetricDataRequest();
				lastPut.setNamespace(pmdr.getNamespace());
				lastPut.setMetricData(allTheMetricDatums);
				// now put objects is ready to send to CloudWatch
				String nextReturnMessage = sendMetrics(lastPut,
						cloudWatchClient);
				// need to clear out the initial list of MetricDatums or
				// will not exit the loop
				allTheMetricDatums.clear();
				toReturn.add(nextReturnMessage);
			}
			// do not want this loop to hog CPU, adding sleep to allow
			// opportunity for
			// switch to other threads
			Thread.yield();
		}
		return toReturn;
	}

	/**
	 * Does "put" to Amazon Web Services CloudWatch. Returns success/ failure
	 * message and registers success/failure with any observing watchers.
	 * 
	 * @param PutMetricDataRequest
	 * @return String for Success/Failure message "put" generates
	 */
	protected String sendMetrics(PutMetricDataRequest listForCW,
			AmazonCloudWatchClient cloudWatchClient) {
		String resultsFromCWPut = "";
		try {
			//System.out.println("hereiswhatthePut " + listForCW.toString());
			// below is the line that sends to CloudWatch
			cloudWatchClient.putMetricData(listForCW);
			// here we were successful with the "put" to CloudWatch
			DateTime timestamp = new DateTime();
			Date jdkDate = timestamp.toDate();
			// reportSuccess initializes the return string
			resultsFromCWPut = reportSuccess(jdkDate);
		} catch (Exception e1) {
			DateTime timestamp = new DateTime();
			Date jdkDate = timestamp.toDate();
			// reportFailure initializes the return string
			resultsFromCWPut = reportFailure(e1.toString(), jdkDate);
	
			log.error("failed to send data to CloudWatch ", e1);
			throw new RuntimeException(e1);
		} finally {
			// want to make sure our success/failure string is returned
			return resultsFromCWPut;
		}
	}

	/**
	 * Creates string representing "put" to CloudWatch success. If parameter is
	 * null, should still send the string.
	 * 
	 * @param Date
	 *            representing when "put" was sent
	 * @return String with Success information
	 */
	public String reportSuccess(Date jdkDate) {
		String toReturn = "SUCCESS PutMetricDataRequest was successfully sent at time "
				+ jdkDate;
		notifyWatchers(toReturn);
		return toReturn;
	}

	/**
	 * Creates string representing "put" to CloudWatch failure.
	 * 
	 * @param errorMessage
	 *            from "put" failure
	 * @param Date
	 *            from when "put" was attempted
	 * @return String with Failure information
	 */
	public String reportFailure(String errorMessage, Date jdkDate) {
		String toReturn = "FAILURE " + errorMessage + " at time " + jdkDate;
		notifyWatchers(toReturn);
		return toReturn;
	}

	/**
	 * adds a Watcher to synchronized list of observing watchers.
	 * 
	 * @param Watcher
	 * @throws IllegalArgumentException
	 *             if parameter object is null
	 */
	public void registerWatcher(Watcher w) {
		if (w == null) {
			throw (new IllegalArgumentException());
		}
		listOfWatchers.add(w);
	}

	/**
	 * Removes watcher from synchronized list of observing watchers.
	 * 
	 * @param Watcher
	 */
	public void removeWatcher(Watcher w) {
		listOfWatchers.remove(w);
	}

	/**
	 * Alerts all observing watcher of success or failure of "put" to
	 * CloudWatch.
	 * 
	 * @param String
	 *            with the success/failure message
	 */
	public void notifyWatchers(String message) {
		for (Watcher w : listOfWatchers) {
			if (!(w instanceof Watcher)) {
				continue;
			}
			w.update(message);
		}
	}

	/**
	 * Getter that returns copy of synchronized list of ProfileData.
	 * 
	 * @return List<ProfileData>
	 */
	protected List<ProfileData> getListProfileData() {
		List<ProfileData> toReturn = new ArrayList<ProfileData>(listProfileData);
		return toReturn;
	}

	/**
	 * Getter that returns copy of synchronized list of Watcher.
	 * 
	 * @return List<Watcher>
	 */
	protected List<Watcher> getWatcherList() {
		List<Watcher> toReturn = new ArrayList<Watcher>(listOfWatchers);
		return toReturn;
	}

	/**
	 * Getter for AmazonCloudWatch client.
	 * 
	 * @return AmazonCloudWatchClient
	 */
	protected AmazonCloudWatchClient getCW() {
		return cloudWatchClient;
	}

	/**
	 * Setter for AmazonCloudWatchClient.
	 * 
	 * @param cw
	 */
	protected void setCloudWatch(AmazonCloudWatchClient cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}
}
