package org.rapla.scheduler;

import io.reactivex.rxjava3.functions.Action;
import org.junit.Assert;
import org.junit.Test;
import org.rapla.logger.ConsoleLogger;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;

public class CommandSchedulerTest
{
    @Test
    public void testDelay() throws InterruptedException {
        final CommandScheduler scheduler = new UtilConcurrentCommandScheduler(new ConsoleLogger());
        final long start = System.currentTimeMillis();
        final AtomicLong result = new AtomicLong();
        Action action = ()-> result.set( System.currentTimeMillis());
        scheduler.delay(action,300 );
        Thread.sleep(800);
        Assert.assertTrue(result.get() - start >= 300);
    }

    @Test
    public void testOldApi() throws ParseException {
        TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault(tz);
        Calendar cal = Calendar.getInstance(tz, Locale.GERMANY);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMANY);

        {
            Date dateBeforeDST = df.parse("2018-03-25 01:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(0, cal.get(Calendar.DST_OFFSET));
        }
        {
            Date dateBeforeDST = df.parse("2018-03-31 01:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(3600000, cal.get(Calendar.DST_OFFSET));
        }

        {
            Date dateBeforeDST = df.parse("2018-09-31 01:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(3600000, cal.get(Calendar.DST_OFFSET));
        }

        {
            Date dateBeforeDST = df.parse("2018-10-28 01:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(3600000, cal.get(Calendar.DST_OFFSET));
        }
        {
            Date dateBeforeDST = df.parse("2018-10-28 02:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(0, cal.get(Calendar.DST_OFFSET));
        }
        {
            Date dateBeforeDST = df.parse("2018-10-31 01:55");
            cal.setTime(dateBeforeDST);
            Assert.assertEquals(3600000, cal.get(Calendar.ZONE_OFFSET));
            Assert.assertEquals(0, cal.get(Calendar.DST_OFFSET));
        }


    }

    @Test
    public void testClock() throws InterruptedException {
        //Clock clock = Clock.systemDefaultZone();
        final ZoneId berlin = ZoneId.of("Europe/Berlin");

        final TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault( timeZone);

        {
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,26), LocalTime.of( 11,15), berlin);
            Clock clock = Clock.fixed(zdt1.toInstant(), berlin);
            long millis = millisToNextPeriod(clock, 12, 0);
            Assert.assertEquals(45,millis/(1000 * 60 ));
        }

        {
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,26), LocalTime.of( 12,15), berlin);
            Clock clock = Clock.fixed(zdt1.toInstant(), berlin);
            long millis = millisToNextPeriod(clock, 11, 0);
            Assert.assertEquals(22*60 + 45,millis/(1000 * 60 ));
            Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( berlin));
            cal.set(2018,Calendar.OCTOBER,26,12,15,0);
            long millis2 = millisToNextPeriod(cal, 11, 0);
            Assert.assertEquals(22 * 60+ 45,millis2/(1000 * 60));

        }

        {
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,27), LocalTime.of( 12,15), berlin);
            Clock clock = Clock.fixed(zdt1.toInstant(), berlin);
            long millis = millisToNextPeriod(clock, 11, 0);
            Assert.assertEquals(23*60 + 45,millis/(1000 * 60 ));
            Calendar cal = Calendar.getInstance(timeZone);
            cal.set(2018,Calendar.OCTOBER,27,12,15,0);
            long millis2 = millisToNextPeriod(cal, 11, 0);
            Assert.assertEquals(23 * 60+ 45,millis2/(1000 * 60));
        }

        {
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,28), LocalTime.of( 12,15), berlin);
            Clock clock = Clock.fixed(zdt1.toInstant(), berlin);
            long millis = millisToNextPeriod(clock, 11, 0);
            Assert.assertEquals(22*60 + 45,millis/(1000 * 60));
            Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( berlin));
            cal.set(2018,Calendar.OCTOBER,28,12,15,0);
            long millis2 = millisToNextPeriod(cal, 11, 0);
            Assert.assertEquals(22 * 60+ 45,millis2/(1000 * 60));
        }

        {
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,29), LocalTime.of( 12,15), berlin);
            Clock clock = Clock.fixed(zdt1.toInstant(), berlin);
            long millis = millisToNextPeriod(clock, 11, 0);
            Assert.assertEquals(22*60 + 45,millis/(1000 * 60));
            Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( berlin));
            cal.set(2018,Calendar.OCTOBER,29,12,15,0);
            long millis2 = millisToNextPeriod(cal, 11, 0);
            Assert.assertEquals(22 * 60+ 45,millis2/(1000 * 60));
        }

        {
            Clock clock1 = Clock.fixed(Instant.parse("2018-10-27T10:15:30.00Z"), berlin);
            Clock clock2 = Clock.fixed(Instant.parse("2018-10-28T11:15:30.00Z"), berlin);
            ZonedDateTime zdt1 = ZonedDateTime.of(LocalDate.of(2018,10,27), LocalTime.of( 12,15), berlin);
            ZonedDateTime zdt2 = ZonedDateTime.of(LocalDate.of(2018,10,28), LocalTime.of( 12,15), berlin);
            //Clock clock1 = Clock.fixed(zdt1.toInstant(), berlin);
            //Clock clock2 = Clock.fixed(zdt2.toInstant(), berlin);
            Assert.assertEquals(25,HOURS.between( zdt1, zdt2));
            Assert.assertEquals(25,HOURS.between( clock1.instant(), clock2.instant()));
            long millis = millisToNextPeriod(clock1, 11, 0);

            Assert.assertEquals(23 * 60+ 45,millis/(1000 * 60));
            Calendar cal = Calendar.getInstance(timeZone);
            cal.set(2018,Calendar.OCTOBER,27,12,15,0);
            long millis2 = millisToNextPeriod(cal, 11, 0);
            Assert.assertEquals(23 * 60+ 45,millis2/(1000 * 60));
        }

    }

    private long millisToNextPeriod(Clock clock, int hourOfDay, int minute) {
        ZonedDateTime now = ZonedDateTime.now( clock);
        ZonedDateTime time = now.withHour(hourOfDay).withMinute( minute);
        ZonedDateTime tommorow = now.plus(Period.ofDays(1));
        long millis = MILLIS.between( now, time);
        if ( millis < 0 )
        {
            ZonedDateTime tommorowTime = tommorow.withHour(hourOfDay).withMinute( minute);
            millis = MILLIS.between( now, tommorowTime);
        }
        return millis;
    }

    private long millisToNextPeriod(Calendar calendar, int hourOfDay, int minute) {
        long now = calendar.getTimeInMillis();
        Calendar clone = (Calendar)calendar.clone();

        clone.set(Calendar.HOUR_OF_DAY, hourOfDay);
        clone.set(Calendar.MINUTE, minute);
        long millis = clone.getTimeInMillis() - now;

        if ( millis < 0 )
        {
            clone.add(Calendar.DATE,1);
            millis =  clone.getTimeInMillis() - now;
//            int offsetNow = calendar.get(Calendar.DST_OFFSET);
//            int offsetTommorow = clone.get(Calendar.DST_OFFSET);
//            int offsetDiff = offsetTommorow - offsetNow;
        }
        return millis;
    }

}
