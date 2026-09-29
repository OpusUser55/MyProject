package dev.ooga.client.module;

import dev.ooga.client.ui.render.Icon;

public enum Category {
	COMBAT("Combat", Icon.COMBAT),
	MOVEMENT("Movement", Icon.MOVEMENT),
	RENDER("Render", Icon.RENDER),
	WORLD("World", Icon.WORLD),
	MISC("Misc", Icon.MISC),
	HUD("HUD", Icon.HUD),
	CLIENT("Client", Icon.CLIENT);

	private final String displayName;
	private final Icon icon;

	Category(String displayName, Icon icon) {
		this.displayName = displayName;
		this.icon = icon;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Icon getIcon() {
		return icon;
	}
}
