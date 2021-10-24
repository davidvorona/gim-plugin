package com.gimp.tasks;

interface GIMPTaskRunner
{
	void run();

	long delay();
}

public class GIMPTask implements GIMPTaskRunner
{
	final public long period;

	public GIMPTask(long defaultPeriod)
	{
		super();
		period = defaultPeriod;
	}

	/**
	 * Runs code that can be overridden, meant to be called
	 * after a delay.
	 */
	public void run()
	{
	}

	/**
	 * Returns the delay, can be overridden for a dynamic delay.
	 *
	 * @return duration in milliseconds by which to delay task
	 */
	public long delay()
	{
		return period;
	}
}
