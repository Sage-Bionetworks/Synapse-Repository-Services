package org.sagebionetworks.docusign;

import java.time.Instant;
import java.util.List;

import com.docusign.esign.model.Envelope;

/**
 * The envelopes that reached a terminal status within a window, and where the next window should start.
 *
 * @param envelopes the envelopes found, each with its envelope ID and status
 * @param queriedOn the signing service's own time for the query, which is where the next window should
 *                  start so that nothing changing while this window was read is missed
 */
public record EnvelopeStatusChanges(List<Envelope> envelopes, Instant queriedOn) {
}
