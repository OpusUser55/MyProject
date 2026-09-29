package dev.ooga.client.media;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Base for sources that poll a command-line tool once a second. */
abstract class PollingMediaSource implements MediaSource {
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "Ooga Media Poll");
		thread.setDaemon(true);
		return thread;
	});

	private volatile MediaInfo current;
	private ScheduledFuture<?> task;

	@Override
	public synchronized void start() {
		if (task != null) return;
		task = SCHEDULER.scheduleWithFixedDelay(() -> {
			try {
				current = poll();
			} catch (RuntimeException e) {
				current = null;
			}
		}, 0, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public synchronized void stop() {
		if (task != null) task.cancel(false);
		task = null;
		current = null;
	}

	@Override
	public MediaInfo current() {
		return current;
	}

	protected abstract MediaInfo poll();

	static double parseDouble(String text) {
		try {
			return Double.parseDouble(text.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
