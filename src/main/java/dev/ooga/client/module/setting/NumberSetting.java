package dev.ooga.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
		this(name, description, defaultValue, min, max, step, "");
	}

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	@Override
	protected Double sanitize(Double value) {
		double clamped = Math.max(min, Math.min(max, value));
		if (step > 0) clamped = Math.round(clamped / step) * step;
		// Avoid values like 0.30000000000000004 leaking into the UI and config.
		return Math.round(clamped * 10000.0) / 10000.0;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	/** 0..1 position of the current value within its range. */
	public double getProgress() {
		return max == min ? 0 : (value - min) / (max - min);
	}

	public void setProgress(double progress) {
		set(min + (max - min) * Math.max(0, Math.min(1, progress)));
	}

	public String format() {
		String text;
		if (step >= 1) text = Integer.toString(getInt());
		else if (step >= 0.1) text = String.format("%.1f", value);
		else text = String.format("%.2f", value);
		return text + suffix;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) set(element.getAsDouble());
	}
}
