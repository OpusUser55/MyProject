package dev.ooga.client.media;

import dev.ooga.client.module.ModuleManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Windows' system media session (what the volume flyout shows: Spotify, browsers, Apple Music,
 * …) via the WinRT {@code GlobalSystemMediaTransportControlsSessionManager}, reached through
 * PowerShell so no native code ships with the mod.
 *
 * <p>One long-lived PowerShell process prints a line per second; starting PowerShell each poll
 * would cost far more. The script exits on its own if Minecraft's process disappears.
 */
final class WindowsMediaSource implements MediaSource {
	private static final String PRELUDE = String.join("\n",
			"$ErrorActionPreference='SilentlyContinue'",
			"[Console]::OutputEncoding=[Text.Encoding]::UTF8",
			"Add-Type -AssemblyName System.Runtime.WindowsRuntime",
			"$asTask=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'})[0]",
			"function Await($op,[Type]$t){$task=$asTask.MakeGenericMethod($t).Invoke($null,@($op));$task.Wait(-1)|Out-Null;$task.Result}",
			"$null=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]",
			"$mgr=Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])");

	private static final String LOOP = String.join("\n",
			"while($true){",
			"  if(-not (Get-Process -Id %d -ErrorAction SilentlyContinue)){exit}",
			"  $s=$mgr.GetCurrentSession()",
			"  if($s){",
			"    $p=Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
			"    $t=$s.GetTimelineProperties();$pb=$s.GetPlaybackInfo()",
			"    $age=([DateTimeOffset]::Now-$t.LastUpdatedTime).TotalSeconds",
			"    [Console]::Out.WriteLine('OOGA'+[char]9+($p.Title -replace \"`t\",' ')+[char]9+($p.Artist -replace \"`t\",' ')+[char]9+$t.Position.TotalSeconds+[char]9+$t.EndTime.TotalSeconds+[char]9+$pb.PlaybackStatus+[char]9+$age)",
			"  } else { [Console]::Out.WriteLine('OOGA'+[char]9+'NONE') }",
			"  [Console]::Out.Flush()",
			"  Start-Sleep -Milliseconds 1000",
			"}");

	private volatile MediaInfo current;
	private Process process;
	private Thread reader;

	@Override
	public synchronized void start() {
		if (process != null && process.isAlive()) return;
		String script = PRELUDE + "\n" + String.format(LOOP, ProcessHandle.current().pid());
		try {
			process = new ProcessBuilder(command(script)).redirectError(ProcessBuilder.Redirect.DISCARD).start();
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't start the Windows media helper", e);
			return;
		}
		Process started = process;
		reader = new Thread(() -> read(started), "Ooga Media Reader");
		reader.setDaemon(true);
		reader.start();
	}

	private void read(Process p) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("OOGA\t")) continue;
				current = parse(line.substring(5));
			}
		} catch (IOException ignored) {
			// Process ended.
		}
		current = null;
	}

	private static MediaInfo parse(String line) {
		String[] parts = line.split("\t", -1);
		if (parts.length < 6 || parts[0].equals("NONE") || parts[0].isBlank()) return null;
		double position = number(parts[2]);
		double duration = number(parts[3]);
		boolean playing = parts[4].trim().equalsIgnoreCase("Playing");
		// Players only update the timeline now and then; age says how stale the position is.
		double age = parts.length > 5 ? number(parts[5]) : 0;
		if (playing && age > 0 && age < 600) position += age;
		return new MediaInfo(parts[0].trim(), parts[1].trim(), position, duration, playing, System.currentTimeMillis());
	}

	private static double number(String text) {
		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static List<String> command(String script) {
		String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
		return List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded);
	}

	@Override
	public synchronized void stop() {
		if (process != null) process.destroy();
		process = null;
		current = null;
	}

	@Override
	public MediaInfo current() {
		return current;
	}

	private void control(String method) {
		String script = PRELUDE + "\n$s=$mgr.GetCurrentSession();if($s){Await ($s." + method + "()) ([bool])|Out-Null}";
		ProcessRunner.fireAndForget(command(script));
	}

	@Override
	public void playPause() {
		control("TryTogglePlayPauseAsync");
	}

	@Override
	public void next() {
		control("TrySkipNextAsync");
	}

	@Override
	public void previous() {
		control("TrySkipPreviousAsync");
	}
}
