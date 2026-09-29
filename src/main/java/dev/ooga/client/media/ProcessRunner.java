package dev.ooga.client.media;

import dev.ooga.client.module.ModuleManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Runs short helper commands off the render thread, with timeouts. */
final class ProcessRunner {
	static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "Ooga Media");
		thread.setDaemon(true);
		return thread;
	});

	private ProcessRunner() {
	}

	/** @return stdout, or null if the command failed or timed out. */
	static String run(List<String> command, long timeoutMs) {
		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
			StringBuilder out = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
					process.destroyForcibly();
					return null;
				}
				String line;
				while ((line = reader.readLine()) != null) out.append(line).append('\n');
			}
			return process.exitValue() == 0 ? out.toString().trim() : null;
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	static void fireAndForget(List<String> command) {
		EXECUTOR.execute(() -> {
			if (run(command, 4000) == null) ModuleManager.LOGGER.debug("Media command failed: {}", command);
		});
	}
}
