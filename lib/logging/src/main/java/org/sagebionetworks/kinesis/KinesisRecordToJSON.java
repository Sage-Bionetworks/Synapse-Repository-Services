package org.sagebionetworks.kinesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KinesisRecordToJSON {
	private static ObjectMapper jacksonObjectMapper = new ObjectMapper();
	private static Logger logger = LogManager.getLogger(KinesisRecordToJSON.class);


	public static byte[] toBytes(AwsKinesisLogRecord record){
		try {
			return jacksonObjectMapper.writeValueAsBytes(record);
		} catch (JsonProcessingException e) {
			//should never happen
			logger.error("unexpected error when coverting to JSON ", e);
		}
		return new byte[0];
	}

}

