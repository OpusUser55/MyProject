package dev.ooga.client.module;

import dev.ooga.client.module.impl.basefinding.FinderAlertsModule;
import dev.ooga.client.module.impl.basefinding.HoleEspModule;
import dev.ooga.client.module.impl.basefinding.LightFinderModule;
import dev.ooga.client.module.impl.basefinding.NewChunksModule;
import dev.ooga.client.module.impl.basefinding.SpawnerFinderModule;
import dev.ooga.client.module.impl.basefinding.StorageEspModule;
import dev.ooga.client.module.impl.basefinding.SusChunkFinderModule;
import dev.ooga.client.module.impl.basefinding.TunnelFinderModule;
import dev.ooga.client.module.impl.client.ClickGuiModule;
import dev.ooga.client.module.impl.client.ClientSettings;
import dev.ooga.client.module.impl.client.FindsHudModule;
import dev.ooga.client.module.impl.client.InfoHudModule;
import dev.ooga.client.module.impl.client.KeystrokesModule;
import dev.ooga.client.module.impl.client.ModuleListModule;
import dev.ooga.client.module.impl.client.MusicModule;
import dev.ooga.client.module.impl.client.NotificationsModule;
import dev.ooga.client.module.impl.client.RadarModule;
import dev.ooga.client.module.impl.client.RegionMapModule;
import dev.ooga.client.module.impl.misc.AdminDetectorModule;
import dev.ooga.client.module.impl.combat.AimAssistModule;
import dev.ooga.client.module.impl.combat.AutoTotemModule;
import dev.ooga.client.module.impl.combat.TriggerBotModule;
import dev.ooga.client.module.impl.client.TargetHudModule;
import dev.ooga.client.module.impl.misc.AutoEatModule;
import dev.ooga.client.module.impl.misc.AutoLogModule;
import dev.ooga.client.module.impl.misc.AutoReconnectModule;
import dev.ooga.client.module.impl.misc.FastPlaceModule;
import dev.ooga.client.module.impl.misc.KeyPearlModule;
import dev.ooga.client.module.impl.misc.NameProtectModule;
import dev.ooga.client.module.impl.client.WatermarkModule;
import dev.ooga.client.module.impl.movement.SprintModule;
import dev.ooga.client.module.impl.render.BlockEspModule;
import dev.ooga.client.module.impl.render.EspModule;
import dev.ooga.client.module.impl.render.FreeLookModule;
import dev.ooga.client.module.impl.render.FreecamModule;
import dev.ooga.client.module.impl.render.FullbrightModule;
import dev.ooga.client.module.impl.render.NametagsModule;
import dev.ooga.client.module.impl.render.SlowMineModule;
import dev.ooga.client.module.impl.render.TracersModule;
import dev.ooga.client.module.impl.render.ZoomModule;
import dev.ooga.client.module.impl.world.AutoToolModule;
import dev.ooga.client.module.setting.SettingEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class ModuleManager {
	public static final Logger LOGGER = LoggerFactory.getLogger("Ooga");
	private static final ModuleManager INSTANCE = new ModuleManager();

	private final List<Module> modules = new ArrayList<>();
	private final List<ToggleListener> toggleListeners = new ArrayList<>();
	private final List<Runnable> dirtyListeners = new ArrayList<>();

	public interface ToggleListener {
		void onToggled(Module module, boolean announce);
	}

	private ModuleManager() {
	}

	public static ModuleManager get() {
		return INSTANCE;
	}

	public void init() {
		register(new ClickGuiModule());
		register(new ClientSettings());
		register(new WatermarkModule());
		register(new ModuleListModule());
		register(new NotificationsModule());
		register(new InfoHudModule());
		register(new KeystrokesModule());
		register(new MusicModule());
		register(new FindsHudModule());
		register(new RadarModule());
		register(new RegionMapModule());
		register(new AdminDetectorModule());
		register(new TargetHudModule());

		register(new NameProtectModule());
		register(new AutoReconnectModule());
		register(new AutoLogModule());
		register(new AutoEatModule());
		register(new FastPlaceModule());
		register(new KeyPearlModule());

		register(new TriggerBotModule());
		register(new AimAssistModule());
		register(new AutoTotemModule());

		register(new FreecamModule());
		register(new FullbrightModule());
		register(new ZoomModule());
		register(new EspModule());
		register(new TracersModule());
		register(new NametagsModule());
		register(new BlockEspModule());
		register(new FreeLookModule());
		register(new SlowMineModule());

		register(new StorageEspModule());
		register(new SpawnerFinderModule());
		register(new SusChunkFinderModule());
		register(new LightFinderModule());
		register(new TunnelFinderModule());
		register(new HoleEspModule());
		register(new NewChunksModule());
		register(new FinderAlertsModule());

		register(new AutoToolModule());

		register(new SprintModule());

		for (Module module : modules) module.addListSetting();
		SettingEvents.listen(setting -> markDirty());
	}

	private void register(Module module) {
		modules.add(module);
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getModules(Category category) {
		List<Module> result = new ArrayList<>();
		for (Module module : modules) {
			if (module.getCategory() == category) result.add(module);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		for (Module module : modules) {
			if (module.getClass() == type) return (T) module;
		}
		throw new IllegalArgumentException("Unknown module " + type.getSimpleName());
	}

	public Module byName(String name) {
		for (Module module : modules) {
			if (module.getName().equalsIgnoreCase(name)) return module;
		}
		return null;
	}

	public void addToggleListener(ToggleListener listener) {
		toggleListeners.add(listener);
	}

	public void addDirtyListener(Runnable listener) {
		dirtyListeners.add(listener);
	}

	void onToggled(Module module, boolean announce) {
		for (ToggleListener listener : toggleListeners) listener.onToggled(module, announce);
		markDirty();
	}

	public void markDirty() {
		for (Runnable listener : dirtyListeners) listener.run();
	}

	public void tick() {
		for (Module module : modules) {
			try {
				if (!module.isEnabled()) {
					module.onDisabledTick();
					continue;
				}
				module.onTick();
			} catch (RuntimeException e) {
				LOGGER.error("Module {} crashed while ticking; disabling it", module.getName(), e);
				module.setEnabled(false);
			}
		}
	}

	/** Raw key press from the keyboard handler while no screen is open. */
	public void onKeyPressed(int key) {
		forEach(module -> {
			if (module.getKey() == key) module.onKeybind();
		});
	}

	private void forEach(Consumer<Module> action) {
		// Copy so a module toggled by a key can't upset iteration.
		for (Module module : new ArrayList<>(modules)) action.accept(module);
	}
}
