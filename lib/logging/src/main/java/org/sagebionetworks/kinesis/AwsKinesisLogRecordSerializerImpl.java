package org.sagebionetworks.kinesis;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AwsKinesisLogRecordSerializerImpl implements AwsKinesisLogRecordSerializer {

	//for converting AwsKinesisLogRecord to json
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	@Override
	public byte[] toBytes(AwsKinesisLogRecord record) {
		byte[] jsonBytes;
		
		try {
			jsonBytes = OBJECT_MAPPER.writeValueAsBytes(record);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize record " + record, e);
		}
		
		return jsonBytes;
	}
	
}
