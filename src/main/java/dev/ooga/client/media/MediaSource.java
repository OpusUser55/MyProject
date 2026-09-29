package dev.ooga.client.media;

/** A platform-specific way to read and control the system's current media session. */
public interface MediaSource {
	/** Begin polling in the background. */
	void start();

	void stop();

	/** Latest snapshot, or null when nothing is playing or the platform isn't supported. */
	MediaInfo current();

	void playPause();

	void next();

	void previous();
}
