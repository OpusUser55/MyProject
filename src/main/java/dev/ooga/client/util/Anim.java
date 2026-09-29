package dev.ooga.client.util;

import dev.ooga.client.module.impl.client.ClientSettings;

/**
 * Frame-rate independent smoothing. Each {@code Anim} eases its value toward a target with
 * exponential decay, so UI motion feels identical at 30 or 300 FPS. Every instance keeps its
 * own clock, so it doesn't matter how many systems render in a frame. The global animation
 * speed from Client Settings scales every animation at once.
 */
public final class Anim {
	private float value;
	private float target;
	private final float speed;
	private long lastUpdate = -1;

	/** @param speed roughly "how many times per second the gap closes by ~63%". */
	public Anim(float initial, float speed) {
		this.value = initial;
		this.target = initial;
		this.speed = speed;
	}

	public float update(float target) {
		this.target = target;
		return update();
	}

	public float update() {
		long now = System.nanoTime();
		float dt = lastUpdate < 0 ? 0f : Math.min(0.1f, (now - lastUpdate) / 1_000_000_000f);
		lastUpdate = now;

		float multiplier = ClientSettings.animationSpeed();
		if (multiplier >= 9.99f) {
			value = target;
			return value;
		}
		float factor = 1f - (float) Math.exp(-speed * multiplier * dt);
		value += (target - value) * factor;
		if (Math.abs(target - value) < 0.0005f) value = target;
		return value;
	}

	public float get() {
		return value;
	}

	public float target() {
		return target;
	}

	public void snap(float value) {
		this.value = value;
		this.target = value;
	}

	public boolean settled() {
		return value == target;
	}

	/** Smoothstep easing for 0..1 progress values. */
	public static float ease(float t) {
		t = Math.max(0f, Math.min(1f, t));
		return t * t * (3f - 2f * t);
	}
}
