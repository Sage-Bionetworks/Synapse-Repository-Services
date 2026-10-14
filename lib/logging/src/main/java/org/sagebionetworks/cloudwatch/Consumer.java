package org.sagebionetworks.cloudwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

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
	CloudWatchClient cloudWatchClient;

	/**
	 * No parameter consumer constructor.
	 */
	public Consumer() {
	}

	/**
	 * Consumer constructor that takes a CloudWatch client as parameter.
	 * @param client for Amazon
	 */
	public Consumer(CloudWatchClient cloudWatchClient) {
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
	 * Add a list of ProfileData to be published.
	 * 
	 * @param toAdd
	 */
	public void addProfileData(List<ProfileData> toAdd) {
		listProfileData.addAll(toAdd);
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
				List<MetricDatum> batch = new ArrayList<MetricDatum>();
				for(MetricDatum md: fullList){
					// Add this metric to the batch.
					batch.add(md);
					// When the batch is full send it.
					if(batch.size() == MAX_BATCH_SIZE){
						// Send the batch.
						sendMetrics(key, batch, cloudWatchClient);
						batch = new ArrayList<MetricDatum>();
					}
				}
				// If the batch is not empty then we need to send it
				if(!batch.isEmpty()){
					sendMetrics(key, batch, cloudWatchClient);
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
	
	// for testing only
	public void clearProfileData() {
		this.listProfileData.clear();
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

	// Although the SDK docs state a dimension value can be up to 255 characters, CloudWatch
	// rejects values at that length with a runtime error:
	// 'The parameter MetricData.member.1.Dimensions.member.4.Value must be shorter than 250 characters.'
	private static final int MAX_DIMENSION_STRING_LENGTH_PLUS_ONE = 250;
	
	// CloudWatch restricts strings used in Dimensions to be < 250 characters
	// and throws an error if "contains non-ASCII characters" which seems to
	// arise if you use \n or \t (even though they are, in fact, ascii char's!)
	public static String scrubDimensionString(String s) {
		if (s==null) return null;
		s = s.replaceAll("\\s", " "); // replace all white space with a simple space
		if (s.length()>=MAX_DIMENSION_STRING_LENGTH_PLUS_ONE) 
			s = s.substring(0, MAX_DIMENSION_STRING_LENGTH_PLUS_ONE);
		return s;
	}
	
	/**
	 * Converts a ProfileData to a MetricDatum.
	 * 
	 * @param ProfileData
	 * @return MetricDatum throws IllegalArgumentException if parameter object
	 *         is null
	 */
	public static MetricDatum makeMetricDatum(ProfileData pd) {
		// AmazonWebServices requires the MetricDatum have a namespace
		// and unit can't be smaller than zero as it represents latency
		
		ValidateArgument.required(pd, "profileData");
		ValidateArgument.required(pd.getName(), "profileData.name");

		MetricDatum.Builder builder = MetricDatum.builder()
				.metricName(pd.getName())
				.value(pd.getValue())
				.unit(StandardUnit.fromValue(pd.getUnit()));

		if (pd.getTimestamp() != null) {
			builder.timestamp(pd.getTimestamp().toInstant());
		}

		if (pd.getDimension() != null) {
			List<Dimension> dimensions = pd.getDimension().entrySet().stream().map( entry -> {
				String key = scrubDimensionString(entry.getKey());
				String value = scrubDimensionString(entry.getValue());

				if (StringUtils.isBlank(value)) {
					// See https://sagebionetworks.jira.com/browse/PLFM-7215, dimension values cannot be empty
					log.warn("Skipping empty dimension value: {}.dimensions.{}", pd.getName(), key);
					return null;
				}

				return Dimension.builder()
						.name(key)
						.value(value)
						.build();

			}).filter(Objects::nonNull).collect(Collectors.toList());

			if (!dimensions.isEmpty()) {
				builder.dimensions(dimensions);
			}
		}

		if (pd.getMetricStats() != null) {
			builder.statisticValues(StatisticSet.builder()
					.maximum(pd.getMetricStats().getMaximum())
					.minimum(pd.getMetricStats().getMinimum())
					.sampleCount(pd.getMetricStats().getCount())
					.sum(pd.getMetricStats().getSum())
					.build());
		}
		return builder.build();
	}


	/**
	 * Does "put" to Amazon Web Services CloudWatch for a single namespace batch.
	 *
	 * @param namespace the CloudWatch namespace for the batch
	 * @param metricData the metrics to publish
	 */
	protected void sendMetrics(String namespace, List<MetricDatum> metricData,
			CloudWatchClient cloudWatchClient) {
		PutMetricDataRequest request = PutMetricDataRequest.builder()
				.namespace(namespace)
				.metricData(metricData)
				.build();
		try {
			cloudWatchClient.putMetricData(request);
		} catch (Exception e) {
			log.error("Failed to send data to CloudWatch: {}", request, e);
			throw new RuntimeException(e);
		}
	}


	/**
	 * Getter for the CloudWatch client.
	 *
	 * @return CloudWatchClient
	 */
	protected CloudWatchClient getCW() {
		return cloudWatchClient;
	}

	/**
	 * Setter for the CloudWatch client.
	 *
	 * @param cw
	 */
	protected void setCloudWatch(CloudWatchClient cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}

}
