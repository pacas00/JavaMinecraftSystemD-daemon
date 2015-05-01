package net.petercashel.jmsDd.util;

import net.petercashel.jmsDd.daemonMain;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AutoRestartJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		daemonMain.AutoRestart();
	}

}
