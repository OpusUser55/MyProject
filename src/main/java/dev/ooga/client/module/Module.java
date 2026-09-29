package dev.ooga.client.module;

import dev.ooga.client.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every Ooga feature. Modules never talk to the HUD, ClickGUI, notifications
 * or config directly: toggling goes through {@link ModuleManager}, which fans the change out
 * to every interested system so they always stay in sync.
 */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	/** GLFW key code, or -1 when unbound. */
	private int key = -1;
	/** Hidden modules never show in the HUD module list (e.g. the watermark itself). */
	private boolean hiddenFromList;
	/** Settings-only modules have no on/off state of their own (e.g. Client Settings). */
	private boolean settingsOnly;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	protected void hideFromList() {
		this.hiddenFromList = true;
	}

	/** Starts the module enabled on first launch, without notifications or callbacks. */
	protected void enableByDefault() {
		this.enabled = true;
	}

	protected void settingsOnly() {
		this.settingsOnly = true;
		this.hiddenFromList = true;
	}

	public boolean isSettingsOnly() {
		return settingsOnly;
	}

	protected void setDefaultKey(int key) {
		this.key = key;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** What the module's keybind does. Most modules toggle; the ClickGUI opens itself. */
	public void onKeybind() {
		if (!settingsOnly) toggle();
	}

	public void setEnabled(boolean enabled) {
		setEnabled(enabled, true);
	}

	/**
	 * @param announce whether to raise a notification. Config loading passes false so the
	 *                 client doesn't spam toasts on startup.
	 */
	public void setEnabled(boolean enabled, boolean announce) {
		if (this.enabled == enabled) return;
		if (enabled && !canEnable()) return;

		this.enabled = enabled;
		try {
			if (enabled) onEnable();
			else onDisable();
		} catch (RuntimeException e) {
			// A broken module must never take the whole client down with it.
			this.enabled = false;
			ModuleManager.LOGGER.error("Module {} failed to {}", name, enabled ? "enable" : "disable", e);
		}
		ModuleManager.get().onToggled(this, announce);
	}

	/** Modules that need a world (most of them) can veto enabling from the menu. */
	protected boolean canEnable() {
		return true;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called once per client tick while enabled. */
	public void onTick() {
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
		ModuleManager.get().markDirty();
	}

	/**
	 * Whether the enabled state is restored on the next launch. Session-bound features such as
	 * Freecam return false so the client never starts in an unexpected state.
	 */
	public boolean persistsEnabledState() {
		return true;
	}

	public boolean isHiddenFromList() {
		return hiddenFromList;
	}

	/** Short state shown next to the name in the module list, e.g. a mode. May be null. */
	public String getSuffix() {
		return null;
	}

	protected boolean inWorld() {
		return mc.player != null && mc.level != null;
	}
}
