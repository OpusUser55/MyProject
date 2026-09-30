package dev.ooga.client.world;

/**
 * Server tick rate, estimated from how often time updates arrive. Vanilla servers send one every
 * 20 ticks, so at a healthy 20 TPS they're a second apart; a lagging server stretches the gap.
 */
public final class ServerStats {
	private static final int SAMPLES = 8;
	private static final double[] RATES = new double[SAMPLES];
	private static int count;
	private static int next;
	private static long lastArrival = -1;

	private ServerStats() {
	}

	/** From {@code ClientPacketListenerMixin} on every time update. */
	public static void onTimeUpdate() {
		long now = System.currentTimeMillis();
		if (lastArrival > 0) {
			long gap = now - lastArrival;
			// Ignore bursts (e.g. right after joining) and long stalls that are really disconnects.
			if (gap > 200 && gap < 10_000) {
				RATES[next] = Math.min(20.0, 20.0 * 1000.0 / gap);
				next = (next + 1) % SAMPLES;
				count = Math.min(SAMPLES, count + 1);
			}
		}
		lastArrival = now;
	}

	/** Average TPS over the last few seconds, or -1 before there's enough data. */
	public static double tps() {
		if (count < 2) return -1;
		double sum = 0;
		for (int i = 0; i < count; i++) sum += RATES[i];
		return sum / count;
	}

	/** Seconds since the last time update; large values mean the server has stopped responding. */
	public static double secondsSinceUpdate() {
		return lastArrival < 0 ? 0 : (System.currentTimeMillis() - lastArrival) / 1000.0;
	}

	public static void reset() {
		count = 0;
		next = 0;
		lastArrival = -1;
	}
}
