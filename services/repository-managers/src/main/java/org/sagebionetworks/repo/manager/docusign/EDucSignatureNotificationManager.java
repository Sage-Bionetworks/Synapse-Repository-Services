package org.sagebionetworks.repo.manager.docusign;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeToExamine;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

/**
 * Tells the creator of a data access request when its eDUC signature envelope reaches a terminal state.
 */
public interface EDucSignatureNotificationManager {

	/**
	 * Whether notifying is switched on at all. Checked by the caller so that a disabled feature costs no
	 * database or signing-service traffic.
	 */
	boolean isEnabled();

	/**
	 * The routed envelopes whose terminal state has not been recorded yet.
	 */
	List<EDucEnvelopeToExamine> listEnvelopesToExamine(long limit);

	/**
	 * Acts on the state an envelope was observed to be in.
	 * <p>
	 * A state that is not terminal is ignored, leaving the envelope to be examined again. A terminal state is
	 * recorded so that it is never acted on twice, and the request's creator is notified for those states
	 * they need to know about.
	 *
	 * @param status the state the envelope was observed in, or null if it could not be determined
	 */
	void processObservedStatus(EDucEnvelopeToExamine envelope, EDucStatusEnum status);
}
