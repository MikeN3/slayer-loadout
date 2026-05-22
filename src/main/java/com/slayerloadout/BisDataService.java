package com.slayerloadout;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the bundled best-in-slot dataset ({@code bis-data.json}) and indexes it
 * by monster name for fast lookup.
 *
 * <p>The data file is the only "database" the plugin needs - it is bundled inside
 * the jar and read with {@link Class#getResourceAsStream(String)} as required by
 * the Plugin Hub (plugins are not unpacked to disk).</p>
 */
@Slf4j
@Singleton
public class BisDataService
{
	private final Map<String, MonsterLoadout> byName = new HashMap<>();
	// One display name per monster (its primary task name) for autocomplete suggestions.
	private final java.util.List<String> primaryNames = new java.util.ArrayList<>();
	private boolean loaded = false;

	public synchronized void load()
	{
		if (loaded)
		{
			return;
		}
		loaded = true;

		try (InputStream is = BisDataService.class.getResourceAsStream("bis-data.json"))
		{
			if (is == null)
			{
				log.warn("Slayer Loadout: bis-data.json resource was not found");
				return;
			}

			final Gson gson = new Gson();
			final BisData data = gson.fromJson(
				new InputStreamReader(is, StandardCharsets.UTF_8), BisData.class);

			if (data == null || data.monsters == null)
			{
				log.warn("Slayer Loadout: bis-data.json was empty or malformed");
				return;
			}

			for (MonsterLoadout monster : data.monsters)
			{
				for (String name : monster.getNames())
				{
					byName.put(normalize(name), monster);
				}
				if (!monster.getNames().isEmpty())
				{
					primaryNames.add(monster.getNames().get(0));
				}
			}

			log.debug("Slayer Loadout: loaded loadouts for {} monster aliases", byName.size());
		}
		catch (Exception e)
		{
			log.warn("Slayer Loadout: failed to load bis-data.json", e);
		}
	}

	/**
	 * @param taskName the slayer task / monster name (case-insensitive)
	 * @return the matching loadout, or {@code null} if the task is not in the dataset
	 */
	public MonsterLoadout find(String taskName)
	{
		if (taskName == null)
		{
			return null;
		}

		final String key = normalize(taskName);
		MonsterLoadout exact = byName.get(key);
		if (exact != null)
		{
			return exact;
		}

		// Fall back to a singular/plural tolerant match (e.g. "abyssal demon").
		if (key.endsWith("s"))
		{
			MonsterLoadout singular = byName.get(key.substring(0, key.length() - 1));
			if (singular != null)
			{
				return singular;
			}
		}
		else
		{
			MonsterLoadout plural = byName.get(key + "s");
			if (plural != null)
			{
				return plural;
			}
		}

		return null;
	}

	/**
	 * One name per monster (its primary task name), de-duplicated and sorted, for
	 * autocomplete suggestions. Typing any alias still resolves via {@link #find}.
	 */
	public java.util.List<String> getSuggestionNames()
	{
		return new java.util.ArrayList<>(new java.util.TreeSet<>(primaryNames));
	}

	static String normalize(String s)
	{
		return s == null ? "" : s.toLowerCase(Locale.ENGLISH).trim();
	}
}
