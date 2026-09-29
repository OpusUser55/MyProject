package dev.ooga.client.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/**
 * A single configurable value owned by a module. Settings are rendered by the ClickGUI
 * and persisted by the config system, so every setting knows how to (de)serialize itself.
 */
public abstract class Setting<T> {
	private final String name;
	private final String description;
	protected T value;
	private final T defaultValue;
	private BooleanSupplier visibility = () -> true;
	private Runnable onChange = () -> {};

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		if (value == null || value.equals(this.value)) return;
		this.value = sanitize(value);
		onChange.run();
		SettingEvents.changed(this);
	}

	public void reset() {
		set(defaultValue);
	}

	public T getDefault() {
		return defaultValue;
	}

	protected T sanitize(T value) {
		return value;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
		this.visibility = supplier;
		return (S) this;
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S onChange(Runnable runnable) {
		this.onChange = runnable;
		return (S) this;
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
