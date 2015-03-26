package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityGroup;
import org.sagebionetworks.repo.model.EntityGroupRecord;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Summary;

public class SummaryMarkdownUtilsTest {
	
	Summary summary;
	EntityGroup g1;
	EntityGroupRecord egr1;
	EntityGroupRecord egr2;
	EntityGroupRecord egr3;
	EntityGroupRecord egr4;
	
	@Before
	public void before(){
		summary = new Summary();
		summary.setGroups(new LinkedList<EntityGroup>());
		
		// G1
		EntityGroup g1 = new EntityGroup();
		g1.setDescription("g1 desc");
		g1.setName("g1 name");
		g1.setRecords(new LinkedList<EntityGroupRecord>());
		// egr1
		egr1 = new EntityGroupRecord();
		egr1.setEntityReference(new Reference());
		egr1.getEntityReference().setTargetId("syn123");
		egr1.getEntityReference().setTargetVersionNumber(2L);
		g1.getRecords().add(egr1);
		
		// egr2
		egr2 = new EntityGroupRecord();
		egr2.setEntityReference(new Reference());
		egr2.getEntityReference().setTargetId("syn456");
		egr2.getEntityReference().setTargetVersionNumber(null);
		g1.getRecords().add(egr2);
		// egr3
		egr3 = new EntityGroupRecord();
		egr3.setEntityReference(new Reference());
		egr3.getEntityReference().setTargetId("syn789");
		egr3.getEntityReference().setTargetVersionNumber(null);
		egr3.setNote("some note");
		g1.getRecords().add(egr3);
		
		// egr4
		egr4 = new EntityGroupRecord();
		egr4.setEntityReference(new Reference());
		egr4.getEntityReference().setTargetId("syn789");
		egr4.getEntityReference().setTargetVersionNumber(99L);
		egr4.setNote("has a version and a note");
		g1.getRecords().add(egr3);
		
		summary.getGroups().add(g1);
	}
	
	@Test
	public void testGenerateMarkdown(){
		String result = SummaryMarkdownUtils.generateSummaryMarkdown(summary);
		assertEquals("\n### g1 name\ng1 desc\n${entitylist?list=syn123%2Fversion%2F2%3Bsyn456%3Bsyn789%2Csome note%3Bsyn789%2Csome note}", result);
	}

}
