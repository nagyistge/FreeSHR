package org.freeshr.validations;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateValidatorTest {

    @Test
    public void shouldValidateDate() throws Exception {
        DateValidator dateValidator = new DateValidator();
        Date date = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        assertTrue(dateValidator.isValidDate(date));
    }

    @Test
    public void shouldValidateInvalidDate() throws Exception {
        DateValidator dateValidator = new DateValidator();
        Date Date = new DateTime("2014-02-31T00:00:00+05:30").toDate();
        assertFalse(dateValidator.isValidDate(Date));
    }

    @Test
    public void shouldValidatePeriod() throws Exception {
        DateValidator dateValidator = new DateValidator();

        Date startDate = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        Date endDate = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        assertTrue(dateValidator.isValidPeriod(startDate, endDate));


        startDate = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        endDate = null;
        assertTrue(dateValidator.isValidPeriod(startDate, endDate));


        startDate = null;
        endDate = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        assertTrue(dateValidator.isValidPeriod(startDate, endDate));

    }

    @Test
    public void shouldValidateInvalidPeriod() throws Exception {
        DateValidator dateValidator= new DateValidator();

        Date startDate = new DateTime("2014-12-31T00:00:00+05:30").toDate();
        Date endDate = new DateTime("2014-11-30T00:00:00+05:30").toDate();
        assertFalse(dateValidator.isValidPeriod(startDate, endDate));

    }

}