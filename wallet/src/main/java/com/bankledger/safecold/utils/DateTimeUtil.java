/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecold.utils;

import android.text.format.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtil {

	private DateTimeUtil() {

	}

	public static final String SHORT_DATE_TIME_FORMAT = "MM-dd HH:mm";
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String DATE_TIME_FORMAT_DCIM_FilENAME = "yyyyMMdd_HHmmss";
	private static final String DEFAULT_TIMEZONE = "GMT+0";

    public static final String getNameForDcim(long time) {
        Date date = new Date(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                DATE_TIME_FORMAT_DCIM_FilENAME);
        return dateFormat.format(date);
    }


    public static final String getNameForFile(long time) {
		Date date = new Date(time);
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				DATE_TIME_FORMAT_DCIM_FilENAME);
		return dateFormat.format(date);
	}

	public static final String getDateTimeString(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
		String result = df.format(date);
		return result;
	}

	public static final String getShortDateTimeString(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(SHORT_DATE_TIME_FORMAT);
		return df.format(date);
	}

	public static final long getDateTimeForTime(String str)
			throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
		return new Date(df.parse(str).getTime()).getTime();
	}

	public static final Date getDateTimeForTimeZone(Long time) {
		Long sourceRelativelyGMT = time
				- TimeZone.getTimeZone(DEFAULT_TIMEZONE).getRawOffset();
		Long targetTime = sourceRelativelyGMT
				+ TimeZone.getDefault().getRawOffset();

		Date date = new Date(targetTime);
		return date;

	}

	public static int getDayOfWeek(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		return calendar.get(Calendar.DAY_OF_WEEK);
	}

	public static int getHour(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeMillis);
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

}
