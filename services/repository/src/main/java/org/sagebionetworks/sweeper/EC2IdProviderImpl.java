package org.sagebionetworks.sweeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class EC2IdProviderImpl implements EC2IdProvider {

	private static final String EC2_METADATA_ENDPOINT = "http://169.254.169.254/latest/meta-data";

	@Override
	public String getEC2InstanceId() throws IOException {
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
