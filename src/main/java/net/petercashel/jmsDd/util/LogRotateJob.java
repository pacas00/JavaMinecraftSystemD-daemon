package net.petercashel.jmsDd.util;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class LogRotateJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		PrintStreamHandler.logRotate();
	}

}
