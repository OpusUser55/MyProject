package dev.ooga.client.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts mouse clicks over the last second, for the keystrokes HUD. */
public final class ClickTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private ClickTracker() {
	}

	public static void onPress(int button) {
		long now = System.currentTimeMillis();
		if (button == 0) LEFT.addLast(now);
		else if (button == 1) RIGHT.addLast(now);
	}

	public static int leftCps() {
		return count(LEFT);
	}

	public static int rightCps() {
		return count(RIGHT);
	}

	private static int count(Deque<Long> clicks) {
		long cutoff = System.currentTimeMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
		return clicks.size();
	}
}
