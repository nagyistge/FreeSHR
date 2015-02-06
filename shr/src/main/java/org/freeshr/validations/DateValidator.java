package org.freeshr.validations;


import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Component
public class DateValidator {

    private static final String DATE_FORMAT = "dd-MM-yyyy";

    //NOTE:Assuming:When Both Dates are null or one is null,there is no Period.So Valid one
    public boolean isValidPeriod(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            return (startDate.before(endDate) || (startDate.toString()).equals(endDate.toString()));
        }
        return true;
    }


    public boolean isValidDate(Date date) {

        String dateToValidate = getDateInStringFormat(date);

        try {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
            simpleDateFormat.setLenient(false);
            simpleDateFormat.parse(dateToValidate);

        } catch (ParseException ex) {

            ex.printStackTrace();
            return false;
        }

        return true;


    }

    private String getDateInStringFormat(Date date) {

        DateTime dateTime = new DateTime(date);
        return dateTime.get(DateTimeFieldType.dayOfMonth()) + "-" + dateTime.get(DateTimeFieldType.monthOfYear()) + "-" + dateTime.get(DateTimeFieldType.year());
    }
}
