package org.sagebionetworks.usagemetrics.users;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserProfile;

/**
 * This script extracts all the individual users from the database and writes
 * out a CSV file with name, email address, etc...
 * 
 * @param 0 - Synapse admin username
 * @param 1 - password
 * @param 2 - name of file to write users to
 * 
 * @author dburdick
 * 
 */
public class ExtractUserList {

		
	static final SynapseClientImpl synapse = new SynapseClientImpl();
	
	public static void main(String[] args) throws Exception {
		if(args.length != 3) throw new IllegalArgumentException("Invalid input parameters");
		String username = args[0];
		String password = args[1];
		String outFilePath = args[2];
		
		synapse.login(username, password);
		
		BufferedWriter out = null;
		try {
			File file = new File(outFilePath);
			file.createNewFile();
			FileWriter fstream = new FileWriter(file);
			out = new BufferedWriter(fstream);
			
			out.write(join(new ArrayList<String>(Arrays.asList(new String[] {
					"PrincipalId",
					"Email",
					"DisplayName",
					"FirstName",
					"LastName",
					"Company"						
			})),",") + "\n");
			System.out.println("Extracting users...");
			extractUsers(out);			
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (out != null) out.close();
		}
	}

	private static void extractUsers(BufferedWriter out) throws SynapseException, IOException {
		long numUsers = 0;
		int offset = 0;
		int limit = 100;
		int count=0;
		do {			
			System.out.println("Offset: " + offset);
			PaginatedResults<UserProfile> batch = synapse.getUsers(offset, limit);
			numUsers = batch.getTotalNumberOfResults();
			for(UserProfile lightProfile : batch.getResults()) {				
				UserProfile fullProfile = synapse.getUserProfile(lightProfile.getOwnerId());
				out.write(join(new ArrayList<String>(Arrays.asList(new String[] {
						fullProfile.getOwnerId(),
						fullProfile.getEmail(),
						fullProfile.getUserName(),
						fullProfile.getFirstName(),
						fullProfile.getLastName(),
						fullProfile.getCompany()						
				})),",") + "\n");
				count++;
			}
			offset += limit;
		} while(count < numUsers);
		System.out.println("Users exported: " + count);
	}
	
	private static String join(List<String> parts, String delimeter) {
		String line = "";
		for(String part : parts) {
			if(!line.isEmpty()) line += delimeter;
			line += part; 
		}
		return line;
	}
}







