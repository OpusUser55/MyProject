package dev.ooga.client.media;

import java.util.List;

/** Spotify, falling back to Apple Music, via AppleScript. */
final class MacMediaSource extends PollingMediaSource {
	private static final String QUERY = String.join("\n",
			"on q(appName)",
			"  if application appName is running then",
			"    using terms from application \"Music\"",
			"      tell application appName",
			"        if player state is stopped then return \"\"",
			"        set t to current track",
			"        set d to duration of t",
			"        if appName is \"Spotify\" then set d to d / 1000",
			"        return (name of t) & tab & (artist of t) & tab & (player position as text) & tab & (d as text) & tab & (player state as text)",
			"      end tell",
			"    end using terms from",
			"  end if",
			"  return \"\"",
			"end q",
			"set r to q(\"Spotify\")",
			"if r is \"\" then set r to q(\"Music\")",
			"return r");

	@Override
	protected MediaInfo poll() {
		String out = ProcessRunner.run(List.of("osascript", "-e", QUERY), 2000);
		if (out == null || out.isBlank()) return null;
		String[] parts = out.split("\t", -1);
		if (parts.length < 5) return null;
		// AppleScript may use a comma decimal separator depending on locale.
		double position = parseDouble(parts[2].replace(',', '.'));
		double duration = parseDouble(parts[3].replace(',', '.'));
		boolean playing = parts[4].trim().equalsIgnoreCase("playing");
		return new MediaInfo(parts[0].trim(), parts[1].trim(), position, duration, playing, System.currentTimeMillis());
	}

	private void tell(String command) {
		String script = "if application \"Spotify\" is running then\ntell application \"Spotify\" to " + command
				+ "\nelse if application \"Music\" is running then\ntell application \"Music\" to " + command + "\nend if";
		ProcessRunner.fireAndForget(List.of("osascript", "-e", script));
	}

	@Override
	public void playPause() {
		tell("playpause");
	}

	@Override
	public void next() {
		tell("next track");
	}

	@Override
	public void previous() {
		tell("previous track");
	}
}
