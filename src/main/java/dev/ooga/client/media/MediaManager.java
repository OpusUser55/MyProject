package dev.ooga.client.media;

import java.util.Locale;

/** Picks the media source for this OS and keeps it running only while someone needs it. */
public final class MediaManager {
	private static final MediaManager INSTANCE = new MediaManager();

	private final MediaSource source;
	private boolean running;

	private MediaManager() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) source = new WindowsMediaSource();
		else if (os.contains("mac")) source = new MacMediaSource();
		else source = new LinuxMediaSource();
		Runtime.getRuntime().addShutdownHook(new Thread(source::stop, "Ooga Media Shutdown"));
	}

	public static MediaManager get() {
		return INSTANCE;
	}

	public void setRunning(boolean run) {
		if (run == running) return;
		running = run;
		if (run) source.start();
		else source.stop();
	}

	public MediaInfo current() {
		return running ? source.current() : null;
	}

	public void playPause() {
		source.playPause();
	}

	public void next() {
		source.next();
	}

	public void previous() {
		source.previous();
	}
}
