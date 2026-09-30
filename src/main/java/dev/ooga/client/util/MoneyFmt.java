package dev.ooga.client.util;

import java.util.Locale;

/** Parses and prints money the way economy plugins show it: "1.5k", "$2,500", "3M". */
public final class MoneyFmt {
	private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

	private MoneyFmt() {
	}

	/** @return the amount, or null if the text isn't a number (with optional $, commas and k/m/b/t/q). */
	public static Double parse(String text) {
		if (text == null) return null;
		String s = text.trim().replace(",", "").replace("_", "");
		if (s.startsWith("$")) s = s.substring(1);
		if (s.isEmpty()) return null;

		double multiplier = 1;
		char last = Character.toUpperCase(s.charAt(s.length() - 1));
		for (int i = 1; i < SUFFIXES.length; i++) {
			if (SUFFIXES[i].charAt(0) == last) {
				multiplier = Math.pow(1000, i);
				s = s.substring(0, s.length() - 1);
				break;
			}
		}
		try {
			double value = Double.parseDouble(s) * multiplier;
			return Double.isFinite(value) ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Compact form with up to two decimals, e.g. 1500 → "1.5K", 999 → "999". */
	public static String format(double amount) {
		int tier = 0;
		double shown = amount;
		while (Math.abs(shown) >= 1000 && tier < SUFFIXES.length - 1) {
			shown /= 1000;
			tier++;
		}
		// Rounding can push 999.995K up to "1000K"; step to the next tier instead.
		if (Math.abs(Math.round(shown * 100) / 100.0) >= 1000 && tier < SUFFIXES.length - 1) {
			shown /= 1000;
			tier++;
		}
		String number = String.format(Locale.ROOT, "%.2f", shown);
		if (number.contains(".")) number = number.replaceAll("0+$", "").replaceAll("\\.$", "");
		return number + SUFFIXES[tier];
	}
}
