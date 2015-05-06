package org.sagebionetworks.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Tools for serializing/deserializing JSON objects to/from a form suitable for using in a URL
 * 
 * @author bhoff
 *
 */
public class SerializationUtils {

	private static final String ENCODING_CHARSET = Charset.forName("utf-8").name();
	
	private static final Hex hex = new Hex();
	
	public static String serializeAndHexEncode(JSONEntity jsonEntity) {
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(jsonEntity);
			byte[] hexEncoded = hex.encode(jsonString.getBytes(ENCODING_CHARSET));
			return new String(hexEncoded, ENCODING_CHARSET);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T extends JSONEntity> T hexDecodeAndDeserialize(String token, Class<T> type) {
		try {
			byte[] hexDecoded = hex.decode(token.getBytes(ENCODING_CHARSET));
			String jsonString = new String(hexDecoded, ENCODING_CHARSET);
			return EntityFactory.createEntityFromJSONString(jsonString, type);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (DecoderException e) {
			throw new RuntimeException(e);
		}
	}
}
