package dev.ooga.client.ui.notify;

import dev.ooga.client.util.Anim;

public final class Notification {
	public enum Kind {
		ENABLED, DISABLED, INFO
	}

	final String title;
	final String message;
	final Kind kind;
	final long durationMs;
	long createdAt;
	boolean leaving;
	final Anim slide = new Anim(0f, 16f);
	final Anim stackY = new Anim(-1f, 18f);

	Notification(String title, String message, Kind kind, long durationMs) {
		this.title = title;
		this.message = message;
		this.kind = kind;
		this.durationMs = durationMs;
		this.createdAt = System.currentTimeMillis();
	}

	float lifeProgress() {
		return Math.min(1f, (System.currentTimeMillis() - createdAt) / (float) durationMs);
	}
}
