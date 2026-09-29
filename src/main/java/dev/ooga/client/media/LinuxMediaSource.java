package dev.ooga.client.media;

import java.util.List;

/** MPRIS players (Spotify, browsers, VLC, …) through {@code playerctl}. */
final class LinuxMediaSource extends PollingMediaSource {
	private static final String FORMAT = "{{title}}\t{{artist}}\t{{position}}\t{{mpris:length}}\t{{status}}";

	@Override
	protected MediaInfo poll() {
		String out = ProcessRunner.run(List.of("playerctl", "metadata", "--format", FORMAT), 1500);
		if (out == null || out.isBlank()) return null;
		String[] parts = out.split("\t", -1);
		if (parts.length < 5 || parts[0].isBlank()) return null;
		// playerctl reports microseconds.
		double position = parseDouble(parts[2]) / 1_000_000.0;
		double duration = parseDouble(parts[3]) / 1_000_000.0;
		boolean playing = parts[4].trim().equalsIgnoreCase("Playing");
		return new MediaInfo(parts[0].trim(), parts[1].trim(), position, duration, playing, System.currentTimeMillis());
	}

	@Override
	public void playPause() {
		ProcessRunner.fireAndForget(List.of("playerctl", "play-pause"));
	}

	@Override
	public void next() {
		ProcessRunner.fireAndForget(List.of("playerctl", "next"));
	}

	@Override
	public void previous() {
		ProcessRunner.fireAndForget(List.of("playerctl", "previous"));
	}
}
