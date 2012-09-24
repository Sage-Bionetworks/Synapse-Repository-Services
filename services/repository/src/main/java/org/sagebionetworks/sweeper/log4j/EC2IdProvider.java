package org.sagebionetworks.sweeper.log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class EC2IdProvider {

	private static final String EC2_METADATA_ENDPOINT = "http://169.254.169.254/latest/meta-data";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long start = System.nanoTime();
		String id = getId();
		long end = System.nanoTime();

		long elapsed = end - start;
		System.out.format("Retrieved id: %s in %d milliseconds%n", id, elapsed/1000000);
	}

	public static String getId() {
		try {
			return getEC2InstanceId();
		} catch (IOException e) {
			return java.util.UUID.randomUUID().toString();
		}
	}

	private static String getEC2InstanceId() throws IOException {
		String ec2Id = "";
		String inputLine;

		URL ec2MetaData = new URL(EC2_METADATA_ENDPOINT+"/instance-id");
		URLConnection EC2MD = ec2MetaData.openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(EC2MD.getInputStream()));
		while ((inputLine = in.readLine()) != null) {
			ec2Id = inputLine;
		}
		in.close();

		return ec2Id;
	}
}
