package dev.ooga.client.media;

/**
 * A snapshot of what's playing. Players report position only occasionally, so the snapshot
 * remembers when it was taken and extrapolates while playback is running.
 */
public record MediaInfo(String title, String artist, double positionSeconds, double durationSeconds, boolean playing, long sampledAt) {
	public double positionAt(long nowMillis) {
		double position = positionSeconds;
		if (playing) position += (nowMillis - sampledAt) / 1000.0;
		return durationSeconds > 0 ? Math.min(durationSeconds, Math.max(0, position)) : Math.max(0, position);
	}

	public double progressAt(long nowMillis) {
		return durationSeconds > 0 ? positionAt(nowMillis) / durationSeconds : 0;
	}

	public boolean sameTrack(MediaInfo other) {
		return other != null && title.equals(other.title) && artist.equals(other.artist);
	}

	public static String formatTime(double seconds) {
		int total = (int) Math.max(0, Math.round(seconds));
		return (total / 60) + ":" + String.format("%02d", total % 60);
	}
}
