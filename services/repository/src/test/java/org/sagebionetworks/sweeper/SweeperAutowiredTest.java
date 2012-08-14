package org.sagebionetworks.sweeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Date;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:logSweeperTest-spb.xml")
public class SweeperAutowiredTest {

	@Resource(name="stackConfiguration.logSweeperCronExpression")
	String cronExpression;
	
	@Autowired
	Sweeper sweeper;

	@SuppressWarnings("deprecation")
	@Test
	public void testCronExpression() throws ParseException {
		AmazonS3 mockS3 = mock(AmazonS3.class);
		sweeper.setClient(mockS3);
		assertNotNull(cronExpression);
		CronExpression expression = new CronExpression(cronExpression);

		Date date = new Date();
		Date nextValidTimeAfter = expression.getNextValidTimeAfter(date);

		if (date.getMinutes() < 10) {
			assertEquals(date.getHours(), nextValidTimeAfter.getHours());
		} else {
			assertEquals(date.getHours()+1, nextValidTimeAfter.getHours());
		}

	}
}
