package dev.ooga.client.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A countdown in ticks with a random length between a minimum and maximum, so automated
 * actions don't happen on a perfectly regular beat.
 */
public final class Delay {
	private int left;

	/** Starts a new wait of min..max ticks (inclusive). */
	public void start(int min, int max) {
		left = min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	/** Counts one tick down; true once the wait is over. */
	public boolean tick() {
		if (left > 0) left--;
		return left <= 0;
	}

	public boolean done() {
		return left <= 0;
	}

	public void clear() {
		left = 0;
	}

	/** True with the given percent chance (0-100). */
	public static boolean chance(double percent) {
		return percent >= 100 || ThreadLocalRandom.current().nextDouble(100) < percent;
	}
}
