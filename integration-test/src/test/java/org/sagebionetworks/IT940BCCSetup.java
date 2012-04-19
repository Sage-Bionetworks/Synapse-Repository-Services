package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.bccsetup.BccConfigHelper;
import org.sagebionetworks.bccsetup.SpreadsheetHelper;

// Note this depends on a test Google spreadsheet to exist, accessible by the user 'synapse@sagebase.org' 
// and to be named according to the stack configuration (for testing it's "Test BCC Registrants")
public class IT940BCCSetup {

	@Test
	public void testBCCSignUpSheet() {
		  SpreadsheetHelper ssh = new SpreadsheetHelper();
		  String spreadsheetTitle = BccConfigHelper.getBCCSpreadsheetTitle();
		  
		  // in its initial state there is just one participant for whom to allocate resources
		  List<String> participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(1, participantsToAllocate.size());
		  String participant = participantsToAllocate.iterator().next();
		  ssh.recordAllocation(participant, "<timestamp>"); // would use an actual time stamp here
		  ssh.setCellValue(spreadsheetTitle, participant, "VM NAME", "foo"); // set some other field
		  
		  // now there should be no participants for whom to allocate resources
		  participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(0, participantsToAllocate.size());
		  
		  // now reset
		  ssh.setCellValue(spreadsheetTitle, participant, SpreadsheetHelper.ALLOCATED_COLUMN_TITLE, null);
		  ssh.setCellValue(spreadsheetTitle, participant, "VM NAME", null);

		  // as before
		  participantsToAllocate = ssh.getParticipantsToAllocate();
		  assertEquals(1, participantsToAllocate.size());
	}

	private static final String TEST_SHEET_NAME = "Integration Test Sheet";
	
	@Test
	public void testGenericSheetOperations() throws Exception {
		  SpreadsheetHelper ssh = new SpreadsheetHelper();
		  
		  // add a row
		  Map<String,String> rowValues = new HashMap<String,String>();
		  String emailAddressHeader = "Registrant Email Address";
		  String email = "test@foo.com";
		  String valueHeader = "a column";
		  String value = "foo";
		  rowValues.put(emailAddressHeader, email);
		  rowValues.put(valueHeader, value);
		  ssh.addSpreadsheetRow(TEST_SHEET_NAME, rowValues);
		  // check that row is present by looking up the value
		  assertEquals(value, ssh.getCellValue(TEST_SHEET_NAME, email, valueHeader));
		  // now delete the row
		  ssh.deleteSpreadshetRow(TEST_SHEET_NAME, emailAddressHeader, email);
		  try {
			  ssh.getCellValue(TEST_SHEET_NAME, email, valueHeader);
			  fail("Failed to delete spreadsheet row");
		  } catch (IllegalStateException e) {
			  // as expected
		  }
	}
}
