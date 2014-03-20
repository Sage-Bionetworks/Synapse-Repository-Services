package org.sagebionetworks.cloudwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;

/**
 * Sends metric information to AmazonWebServices CloudWatch. It's the consumer
 * in the producer/consumer pattern and it handles the Watchers in the Observer
 * pattern. Watchers can monitor success or failure of "puts" to CloudWatch
 * 
 * @author ntiedema
 */
public class Consumer {
	static private Logger log = LogManager.getLogger(Consumer.class);
	
	public static final int MAX_BATCH_SIZE = 20;

	// We us an atomic reference to the list instead of using synchronization.
	private ConcurrentLinkedQueue<ProfileData> listProfileData = new ConcurrentLinkedQueue<ProfileData>();

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
			List<ProfileData> nextBunch = pollListFromQueue();

			//here I have a list of potentially different namespaces
			//convert to a map (key is namespace, value is list of metricDatums)
			Map<String, List<MetricDatum>> allTheNamespaces = getAllNamespaces(nextBunch);
			//need to collect the messages for testing
			List<String> toReturn = new ArrayList<String>();
			// We can only send a batch of twenty at a time
			for (String key : allTheNamespaces.keySet()){
				List<MetricDatum> fullList = allTheNamespaces.get(key);
				PutMetricDataRequest batch = null;
				for(MetricDatum md: fullList){
					// If we do not have a create one
					if(batch == null){
						batch = new PutMetricDataRequest();
						batch.setNamespace(key);
					}
					// Add this metric to the batch.
					batch.getMetricData().add(md);
					// When the batch is full send it.
					if(batch.getMetricData().size() == MAX_BATCH_SIZE){
						// Send the batch.
						sendMetrics(batch, cloudWatchClient);
						batch = null;
					}
				}
				// If the batch is not null then we need to send it
				if(batch != null){
					sendMetrics(batch, cloudWatchClient);
				}
			}
			//this will have a message for each batch sent to CloudWatch
			//if no batches were sent to CloudWatch this will have size of 0
			return toReturn;
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}
	/**
	 * Poll all data currently on the queue and add it to a list.
	 * @return
	 */
	private List<ProfileData> pollListFromQueue(){
		List<ProfileData> list = new LinkedList<ProfileData>();
		for(ProfileData pd = this.listProfileData.poll(); pd != null; pd = this.listProfileData.poll()){
			// Add to the list
			list.add(pd);
		}
		return list;
	}

	/**
	 * Returns a map of namespaces, with value being list of each MetricDatum
	 * parameter list contained for that namespace.
	 * 
	 * @param list
	 *            <ProfileData>
	 * @return Map<String, List<MetricDatum>>
	 */
	public Map<String, List<MetricDatum>> getAllNamespaces(List<ProfileData> list) {
		// need return map
		Map<String, List<MetricDatum>> toReturn = new HashMap<String, List<MetricDatum>>();
		// loop through the list
		for (ProfileData pd : list) {
			List<MetricDatum> listMD = toReturn.get(pd.getNamespace());
			if(listMD == null){
				listMD = new ArrayList<MetricDatum>();
				toReturn.put(pd.getNamespace(), listMD);
			}
			// Convert this to a metric and add it to the list.
			listMD.add(makeMetricDatum(pd));
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
	public static MetricDatum makeMetricDatum(ProfileData pd) {
		//AmazonWebServices requires the MetricDatum have a namespace
		//and unit can't be smaller than zero as it represents latency
		if (pd == null) throw new IllegalArgumentException("ProfileData cannot be null");
		if(pd.getName() == null) throw new IllegalArgumentException("ProfileData.name cannot be null");
		MetricDatum toReturn = new MetricDatum();
		toReturn.setMetricName(pd.getName());
		toReturn.setValue(pd.getValue());
		toReturn.setUnit(StandardUnit.valueOf(pd.getUnit()));
		toReturn.setTimestamp(pd.getTimestamp());
		List<Dimension> dimensions = new ArrayList<Dimension>();
		if (pd.getDimension()!=null) {
			for (String key : pd.getDimension().keySet()) {
				Dimension dimension = new Dimension();
				dimension.setName(key);
				dimension.setValue(pd.getDimension().get(key));
				dimensions.add(dimension);
			}
			toReturn.setDimensions(dimensions);
		}
		if (pd.getMetricStats()!=null) {
			StatisticSet statisticValues = new StatisticSet();
			statisticValues.setMaximum(pd.getMetricStats().getMaximum());
			statisticValues.setMinimum(pd.getMetricStats().getMinimum());
			statisticValues.setSampleCount(pd.getMetricStats().getCount());
			statisticValues.setSum(pd.getMetricStats().getSum());
			toReturn.setStatisticValues(statisticValues);
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
	protected void sendMetrics(PutMetricDataRequest listForCW,
			AmazonCloudWatchClient cloudWatchClient) {
		try {
			//System.out.println("hereiswhatthePut " + listForCW.toString());
			// below is the line that sends to CloudWatch
			cloudWatchClient.putMetricData(listForCW);
		} catch (Exception e1) {
			log.error("failed to send data to CloudWatch ", e1);
			throw new RuntimeException(e1);
		}
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
