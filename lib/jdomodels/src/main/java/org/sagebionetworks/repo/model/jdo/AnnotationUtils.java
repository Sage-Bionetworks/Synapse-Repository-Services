package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.UnmodifiableXStream;

public class AnnotationUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.omitField(Annotations.class, "id")
			.omitField(Annotations.class, "etag")
			.alias("annotations", Annotations.class)
			.build();

	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException
	 */
	public static byte[] compressAnnotationsV1(Annotations dto) throws IOException{
		return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto == null || dto.isEmpty() ? null : dto);
	}

	/**
	 * Read the compressed (zip) byte array into the Annotations.
	 * @param zippedBytes
	 * @return the resurrected Annotations
	 * @throws IOException
	 */
	public static Annotations decompressedAnnotationsV1(byte[] zippedBytes) throws IOException{
		Object o = JDOSecondaryPropertyUtils.decompressObject(X_STREAM, zippedBytes);
		if (o==null) return new Annotations();
		return (Annotations) o;
	}


}
