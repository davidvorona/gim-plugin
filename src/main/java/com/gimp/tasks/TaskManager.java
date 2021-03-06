/*
 * Copyright (c) 2021, David Vorona <davidavorona@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gimp.tasks;

import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskManager
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
	public void schedule(Task task, long delay)
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
