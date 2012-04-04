package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.bccsetup.SpreadsheetHelper;

// Note this depends on a test Google spreadsheet to exist, accessible by the user 'synapse@sagebase.org' 
// and to be named according to the stack configuration (for testing it's "Test BCC Registrants")
public class IT940BCCSetup {

	@Test
	public void test() {
		  SpreadsheetHelper ssh = new SpreadsheetHelper();
		  
		  // in its initial state there is just one participant for whom to allocate resources
		  List<String> participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(1, participantsToAllocate.size());
		  String participant = participantsToAllocate.iterator().next();
		  ssh.recordAllocation(participant, "<timestamp>"); // would use an actual time stamp here
		  ssh.setCellValue(participant, "VM NAME", "foo"); // set some other field
		  
		  // now there should be no participants for whom to allocate resources
		  participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(0, participantsToAllocate.size());
		  
		  // now reset
		  ssh.setCellValue(participant, SpreadsheetHelper.ALLOCATED_COLUMN_TITLE, null);
		  ssh.setCellValue(participant, "VM NAME", null);

		  // as before
		  participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(1, participantsToAllocate.size());
	}

}
