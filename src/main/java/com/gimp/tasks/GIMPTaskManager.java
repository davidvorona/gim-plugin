package com.gimp.tasks;

import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GIMPTaskManager
{
	@Inject
	private Timer timer;

	/**
	 * Schedules a task to run after the delay and then schedules
	 * it again after calculating the next delay.
	 *
	 * @param task  task to run after a delay and then reschedule
	 * @param delay initial duration in milliseconds by which to delay task
	 */
	public void schedule(GIMPTask task, long delay)
	{
		TimerTask timerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				task.run();
				long nextDelay = task.delay();
				if (nextDelay != 0)
				{
					schedule(task, nextDelay);
				}
			}
		};
		timer.schedule(timerTask, delay);
	}

	/**
	 * Purges any canceled tasks and cancels the timer, then
	 * creates a new one.
	 */
	public void resetTasks()
	{
		timer.purge();
		timer.cancel();
		timer = new Timer();
	}
}
