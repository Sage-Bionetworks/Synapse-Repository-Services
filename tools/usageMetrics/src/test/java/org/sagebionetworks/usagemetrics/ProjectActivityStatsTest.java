package org.sagebionetworks.usagemetrics;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

public class ProjectActivityStatsTest {

	@Test
	public void test() {
		Collection<String> names = new HashSet<String>();
		names.addAll(Arrays.asList(new String[]{"brian.bot@sagebase.org", "brig.mecham@sagebase.org"}));
		assertFalse(ProjectActivityStats.maxContributorScore(names));
	}

}
