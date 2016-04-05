package org.rapla.rest.client.internal.isodate;


import java.util.Date;

/**
 * Created by Christopher on 02.09.2015.
 */
public class ISODateTimeFormat
{
    public static final ISODateTimeFormat INSTANCE = new ISODateTimeFormat();

    public Date parseTimestamp(String format)
    {
        try
        {
            return SerializableDateTimeFormat.INSTANCE.parseTimestamp(format);
        }
        catch (Exception ex)
        {
            throw  new IllegalArgumentException(ex.getMessage());
        }
    }

    public String formatTimestamp(Date date)
    {
        return SerializableDateTimeFormat.INSTANCE.formatTimestamp( date );
    }
}
