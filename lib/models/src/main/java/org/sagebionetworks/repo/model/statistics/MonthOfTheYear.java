package org.sagebionetworks.repo.model.statistics;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a month in a specific year
 * 
 * @author Marco
 *
 */
public class MonthOfTheYear {

	private int year;
	private int month;

	private MonthOfTheYear(int year, int month) {
		this.year = year;
		this.month = month;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public LocalDate toDate() {
		return LocalDate.of(year, month, 1);
	}

	public static MonthOfTheYear valueOf(LocalDate date) {
		return new MonthOfTheYear(date.getYear(), date.getMonthValue());
	}
	
	public static MonthOfTheYear of(int year, int month) {
		return new MonthOfTheYear(year, month);
	}

	@Override
	public int hashCode() {
		return Objects.hash(month, year);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MonthOfTheYear other = (MonthOfTheYear) obj;
		return month == other.month && year == other.year;
	}

	@Override
	public String toString() {
		return "MonthOfTheYear [year=" + year + ", month=" + month + "]";
	}
	
	

}
