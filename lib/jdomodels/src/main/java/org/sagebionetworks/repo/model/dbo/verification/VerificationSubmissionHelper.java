package org.sagebionetworks.repo.model.dbo.verification;

import java.io.IOException;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class VerificationSubmissionHelper {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(VerificationSubmission.class).build();

	public static byte[] serializeDTO(VerificationSubmission dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static VerificationSubmission deserializeDTO(byte[] serialized) {
		try {
			return (VerificationSubmission)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, serialized);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

}
