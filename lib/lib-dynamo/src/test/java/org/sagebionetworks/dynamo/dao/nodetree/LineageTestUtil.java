package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.dynamo.KeyValueSplitter;

/**
 * Test utils.
 */
class LineageTestUtil {

	/**
	 * Accepts strings like "a#D 1#b" which is a parent-to-child pointer from parent a to child b.
	 */
	static final Pattern LINEAGE_PAIR_SINGLE_LETTER_PATTERN = Pattern.compile(
			"(\\w" + KeyValueSplitter.SEPARATOR +
			"[" + LineageType.ANCESTOR.getType() + LineageType.DESCENDANT.getType() +
			"])\\s+(\\d" + KeyValueSplitter.SEPARATOR + "\\w)");

	/**
	 * Accepts strings like "a#D 1#b" which is a parent-to-child pointer from parent a to child b.
	 * Node IDs will be replaced by those mapped in the hash map.
	 */
	static NodeLineagePair parse(String lineagePairStr, int ancestorDepth, Map<String, String> idMap) {

		if (lineagePairStr == null) {
			throw new NullPointerException();
		}
		if (idMap == null) {
			throw new NullPointerException();
		}

		Matcher matcher = LINEAGE_PAIR_SINGLE_LETTER_PATTERN.matcher(lineagePairStr);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(lineagePairStr);
		}

		DboNodeLineage dbo = new DboNodeLineage();
		dbo.setHashKey(matcher.group(1));
		dbo.setRangeKey(matcher.group(2));
		NodeLineage lineage = new NodeLineage(dbo);

		// Replace with random IDs
		String nodeId = lineage.getNodeId();
		String targetId = lineage.getAncestorOrDescendantId();
		String newNodeId = idMap.get(nodeId);
		if (newNodeId == null) {
			String msg = lineagePairStr;
			msg += " has node name, "; 
			msg += nodeId;
			msg += ", which not contained in the supplied ID map.";
			throw new IllegalArgumentException(msg);
		}

		String newTargetId = idMap.get(targetId);
		if (newTargetId == null) {
			String msg = lineagePairStr;
			msg += " has node name, "; 
			msg += targetId;
			msg += ", which not contained in the supplied ID map.";
			throw new IllegalArgumentException(msg);
		}

		String ancestorId;
		String descendantId;
		if (LineageType.ANCESTOR.equals(lineage.getLineageType())) {
			ancestorId = newTargetId;
			descendantId = newNodeId;
		} else {
			ancestorId = newNodeId;
			descendantId = newTargetId;
		}

		return new NodeLineagePair(ancestorId, descendantId,
				ancestorDepth, lineage.getDistance(), new Date());
	}
}
