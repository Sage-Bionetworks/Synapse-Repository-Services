package org.sagebionetworks.repo.model.dbo.dao.statistics;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StatisticsMonthlyStatusDAOTest {

	@Autowired
	private StatisticsMonthlyDAOImpl dao;
	
	
}
