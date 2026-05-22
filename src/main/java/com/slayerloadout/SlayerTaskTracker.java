package com.slayerloadout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Detects the player's current slayer task by parsing game chat messages.
 *
 * <p>Chat parsing is used (rather than reading internal varbits) because the
 * message formats are stable across game updates and exactly match the user
 * action of "checking" a task at a slayer master, on an enchanted gem, or via
 * the built-in {@code !task} command. Both new-assignment and "currently
 * assigned" (check) phrasings are handled, including Konar's location suffix.</p>
 */
public class SlayerTaskTracker
{
	// Matches e.g.
	//   "You are assigned to kill abyssal demons; only 134 more to go."
	//   "You're assigned to kill 132 aberrant spectres; only 132 more to go."
	//   "You're currently assigned to kill gargoyles; only 122 more to go."
	//   "You are assigned to kill abyssal demons in the Catacombs of Kourend; only 145 more to go."
	private static final Pattern ASSIGNMENT = Pattern.compile(
		"(?:you are assigned to kill|you're assigned to kill|you are now assigned to kill|"
			+ "you have been assigned to kill|you're currently assigned to kill) "
			+ "(?:\\d[\\d,]* )?"
			+ "(?<name>.+?)"
			+ "(?: (?:in|on|at|near|throughout|around|under|inside|within) .+?)?"
			+ "[;,] only (?<amount>[\\d,]+) more to go.*",
		Pattern.CASE_INSENSITIVE);

	private static final Pattern COMPLETED = Pattern.compile(
		"you (?:have completed your task|need a new task)", Pattern.CASE_INSENSITIVE);

	@Getter
	private String currentTask;

	/**
	 * Feed a chat message to the tracker.
	 *
	 * @return {@code true} if this message changed the detected task
	 */
	public boolean parse(String rawMessage)
	{
		if (rawMessage == null)
		{
			return false;
		}

		final String message = stripTags(rawMessage).trim();

		final Matcher m = ASSIGNMENT.matcher(message);
		if (m.matches())
		{
			final String name = m.group("name").trim();
			if (!name.isEmpty() && !name.equalsIgnoreCase(currentTask))
			{
				currentTask = name;
				return true;
			}
			return false;
		}

		if (COMPLETED.matcher(message).find())
		{
			if (currentTask != null)
			{
				currentTask = null;
				return true;
			}
		}

		return false;
	}

	public void setCurrentTask(String task)
	{
		this.currentTask = (task == null || task.trim().isEmpty()) ? null : task.trim();
	}

	/** Remove RuneLite/Jagex colour and image tags from a chat message. */
	private static String stripTags(String s)
	{
		return s.replaceAll("<[^>]*>", "");
	}
}
