package net.thucydides.core.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;

import java.util.Date;

import static net.thucydides.core.matchers.DateMatcherFormatter.formatted;


public class DateIsBeforeMatcher extends TypeSafeMatcher<Date> {

    private final DateTime expectedDate;

    public DateIsBeforeMatcher(final Date expectedDate) {
        this.expectedDate = new DateTime(expectedDate);
    }

    public boolean matchesSafely(Date date) {
        DateTime provided = new DateTime(date);
        return provided.isBefore(expectedDate);
    }

    public void describeTo(Description description) {
        description.appendText("a date that is before ");
        description.appendText(formatted(expectedDate));
    }
}
