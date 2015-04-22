/*******************************************************************************
 *    Copyright 2015 Peter Cashel (pacas00@petercashel.net)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/

package net.petercashel.jmsDd.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// http://stackoverflow.com/questions/20387881/how-to-run-certain-task-every-day-at-a-particular-time-using-scheduledexecutorse
public class DailyRunnerDaemon {
	private final Runnable dailyTask;
	private final int hour;
	private final int minute;
	private final int second;
	private final String runThreadName;

	public DailyRunnerDaemon(Calendar timeOfDay, Runnable dailyTask, String runThreadName) {
		this.dailyTask = dailyTask;
		this.hour = timeOfDay.get(Calendar.HOUR_OF_DAY);
		this.minute = timeOfDay.get(Calendar.MINUTE);
		this.second = timeOfDay.get(Calendar.SECOND);
		this.runThreadName = runThreadName;
	}

	public void start() {
		startTimer();
	}

	private void startTimer() {
		new Timer(runThreadName, true).schedule(new TimerTask() {
			@Override
			public void run() {
				dailyTask.run();
				startTimer();
			}
		}, getNextRunTime());
	}

	private Date getNextRunTime() {
		Calendar startTime = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		startTime.set(Calendar.HOUR_OF_DAY, hour);
		startTime.set(Calendar.MINUTE, minute);
		startTime.set(Calendar.SECOND, second);
		startTime.set(Calendar.MILLISECOND, 0);

		if (startTime.before(now) || startTime.equals(now)) {
			startTime.add(Calendar.DATE, 1);
		}

		return startTime.getTime();
	}
}