package org.sagebionetworks.logging.collate;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

public class LogEventTest {

	@Test
	public void testReflexivity() {
		List<LogEvent> events = setupEventEqualDate(2);
		assertSymmetric(events.get(0), events.get(0));
		assertSymmetric(events.get(1), events.get(1));

		events = setupEventEqualFull(2);
		assertSymmetric(events.get(0), events.get(0));
		assertSymmetric(events.get(1), events.get(1));
	}

	@Test
	public void testSymmetryDate() {
		List<LogEvent> events = setupEventEqualDate(2);
		assertSymmetric(events.get(0), events.get(0));
		assertSymmetric(events.get(1), events.get(1));

		events = setupEventEqualFull(2);
		assertSymmetric(events.get(0), events.get(0));
		assertSymmetric(events.get(1), events.get(1));
	}

	@Test
	public void testTransitivity() {
		List<LogEvent> events = setupEventEqualDate(3);
		assertTransitive(events);

		events = setupEventEqualFull(3);
		assertTransitive(events);
	}

	@Test
	public void testConsistency() {
		List<LogEvent> events = setupEventEqualDate(2);
		assertConsistent(events.get(0), events.get(1));

		events = setupEventEqualFull(2);
		assertConsistent(events.get(0), events.get(1));
	}

	@Test
	public void testCompareToDate() {
		DateTime dateOne = new DateTime();
		DateTime dateTwo = new DateTime(dateOne.plusDays(1));
		LogEvent leOne = new LogEvent(dateOne);
		LogEvent leTwo = new LogEvent(dateTwo);
		assertComparesTo(dateOne, dateTwo, leOne, leTwo);

		leOne = new LogEvent(dateOne, "");
		leTwo = new LogEvent(dateTwo, "");
		assertComparesTo(dateOne, dateTwo, leOne, leTwo);
	}

	@Test
	public void testCompareToDateEqual() {
		DateTime dateOne = new DateTime();
		DateTime dateTwo = new DateTime(dateOne);
		LogEvent leOne = new LogEvent(dateOne);
		LogEvent leTwo = new LogEvent(dateTwo);
		assertComparesTo(dateOne, dateTwo, leOne, leTwo);

		String stringOne = "abc";
		String stringTwo = "ABC";
		leOne = new LogEvent(dateOne, stringOne);
		leTwo = new LogEvent(dateTwo, stringTwo);
		assertComparesTo(stringOne, stringTwo, leOne, leTwo);
	}

	public <T extends Comparable> void assertComparesTo(T compOne, T compTwo, LogEvent leOne, LogEvent leTwo) {
		assertEquals(compOne.compareTo(compTwo), leOne.compareTo(leTwo));
		assertEquals(compTwo.compareTo(compOne), leTwo.compareTo(leOne));
	}

	private void assertConsistent(LogEvent eventOne, LogEvent eventTwo) {
		for (int i = 0; i < 10; ++i) {
			assertTrue("Eventone should always equal eventtwo, no matter how many times it's called.",
					eventOne.equals(eventTwo));
			assertTrue("Eventone should always equal eventtwo, no matter how many times it's called.",
					eventTwo.equals(eventOne));
		}
	}

	@Test
	public void testNullNotEquals() {
		LogEvent logEvent = setupEventEqualDate(1).get(0);
		assertFalse("Non-null should never equal null", logEvent.equals(null));
	}


	private void assertSymmetric(LogEvent eventOne, LogEvent eventTwo) {
		assertTrue("One should equal two", eventOne.equals(eventTwo));
		assertTrue("Two should equal one", eventTwo.equals(eventOne));
		assertEquals(0, eventOne.compareTo(eventTwo));
		assertEquals(0, eventTwo.compareTo(eventOne));
		assertEquals(eventOne.hashCode(), eventTwo.hashCode());
		assertEquals(eventTwo.hashCode(), eventOne.hashCode());
	}

	private void assertTransitive(List<LogEvent> events) {
		int cur, next;
		for (int i = 0; i < events.size(); ++i) {
			cur = i;
			next = (i + 1 < events.size() ? i + i : 0);

			assertTrue(String.format("Event %d should equal %d", cur, next),
					events.get(cur).equals(events.get(next)));
			assertEquals(0, events.get(cur).compareTo(events.get(next)));
			assertEquals(events.get(cur).hashCode(), events.get(next).hashCode());
		}
	}

	private List<LogEvent> setupEventEqualDate(int count) {
		DateTime timeOne = new DateTime();
		List<LogEvent> events = new ArrayList<LogEvent>();
		for (int i = 0; i < count; ++i) {
			events.add(new LogEvent(timeOne));
		}
		return events;
	}

	private List<LogEvent> setupEventEqualFull(int count) {
		DateTime timeOne = new DateTime();
		String line = "a line of test log data. Super fake...";
		List<LogEvent> events = new ArrayList<LogEvent>();
		for (int i = 0; i < count; ++i) {
			events.add(new LogEvent(timeOne, line));
		}
		return events;
	}
}
