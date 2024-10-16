package org.sagebionetworks.repo.scripts;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class TOSUpdateEmailJob {
	
	public static void main(String[] args) throws SQLException, IOException, InterruptedException {
		String from = "Synapse<tos-noreply@synapse.org>";
		String subject = "Updates to our Terms of Service and Privacy Policy";
		String emailTemplatePath = "message/TermsOfServiceUpdateTemplate.html";
		
		String emailCsvFile = args[0];
		int sendLimit = Integer.parseInt(args[1]);
		int maxSendRate = Integer.parseInt(args[2]);
		boolean doSend = Boolean.parseBoolean(args[3]);
		
		try (BasicDataSource dataSource = new BasicDataSource()) {	
			dataSource.setUrl(args[4]);
			dataSource.setUsername(args[5]);
			dataSource.setPassword(args[6]);
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");		
			
			EmailListSender sender = new EmailListSender("TOSUpdate", new JdbcTemplate(dataSource), sendLimit, maxSendRate);
			
			try {
				sender.start(emailCsvFile, from, subject, emailTemplatePath, doSend);
			} finally {
				sender.shutdown();
			}
		}
	}
	
	

}
