package org.sagebionetworks.repo.model.dbo.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

class VerificationSubmissionHelperTest {

	@Test
	void testRoundTrip() {
		VerificationSubmission dto = new VerificationSubmission();
		dto.setCreatedBy("101");
		dto.setCreatedOn(new Date());
		dto.setCompany("company");
		VerificationState state = new VerificationState();
		state.setCreatedBy("101");
		state.setState(VerificationStateEnum.APPROVED);
		dto.setStateHistory(Collections.singletonList(state));

		// method under test
		byte[] bytes = VerificationSubmissionHelper.serializeDTO( dto);

		// method under test
		assertEquals(dto, VerificationSubmissionHelper.deserializeDTO(bytes));
	}

}
