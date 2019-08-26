package org.sagebionetworks.kinesis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AwsKinesisLogRecordSerializerImpl implements AwsKinesisLogRecordSerializer {
	
	private static final Logger LOG = LogManager.getLogger(AwsKinesisLogRecordSerializerImpl.class);

	//for converting AwsKinesisLogRecord to json
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private static final byte[] NEW_LINE_BYTES = "\n".getBytes(StandardCharsets.UTF_8);

	@Override
	public ByteBuffer toBytes(AwsKinesisLogRecord record) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			OBJECT_MAPPER.writeValue(byteArrayOutputStream, record);
			byteArrayOutputStream.write(NEW_LINE_BYTES);
		} catch (IOException e) {
			//should never happen
			LOG.error("unexpected error when coverting to JSON ", e);
		}
		return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
	}
	
}
