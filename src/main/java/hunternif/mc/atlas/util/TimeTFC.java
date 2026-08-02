package hunternif.mc.atlas.util;

import su.tfcsurvival.util.calendar.CalendarTFC;
import su.tfcsurvival.util.calendar.ICalendarFormatted;

public class TimeTFC {

    public static String getTimeAndDate(long time) {
        return ICalendarFormatted.getTimeAndDate(time + (long) (5 * CalendarTFC.CALENDAR_TIME.getDaysInMonth() * 24000 + 6000), CalendarTFC.CALENDAR_TIME.getDaysInMonth());
    }
}
