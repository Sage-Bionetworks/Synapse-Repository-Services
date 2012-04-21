package org.sagebionetworks.client;

import org.apache.log4j.Logger;
import org.sagebionetworks.client.SynapseWikiGenerator;

/**
 * 
 * mvn exec:java
 * -Dexec.mainClass="org.sagebionetworks.client.EntityWikiGenerator"
 * -Dexec.args="-u YourEmail -p YourPassword"| iconv -c -f UTF-8 -t
 * UTF-8 | grep -v "\[INFO\]" | sed "s/ INFO \[org.sagebionetworks.*\] //" | sed
 * 's/DEBUG \[org.apache.http.headers\] << //' | grep -v "DEBUG
 * \[org.apache.http.headers\] >>"
 * 
 */
public class EntityWikiGenerator {

	private static final Logger log = Logger
			.getLogger(EntityWikiGenerator.class.getName());

	/**
	 * @param args
	 * @return the number of errors encountered during execution
	 * @throws Exception
	 */
	public static int main(String[] args) throws Exception {

		SynapseWikiGenerator synapse = SynapseWikiGenerator
				.createFromArgs(args);

		synapse.login();

		log.info("h1. Get an Entity");
		synapse.getEntityById("syn4494");

		log.info("h1. Get an Entity owned by Nicole");
		synapse.getEntityById("syn296249");

		return 0;
	}
}
