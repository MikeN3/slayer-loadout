package com.slayerloadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SlayerTaskTrackerTest
{
	@Test
	public void parsesNewAssignment()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertTrue(t.parse("You are assigned to kill abyssal demons; only 134 more to go."));
		assertEquals("abyssal demons", t.getCurrentTask());
	}

	@Test
	public void parsesLeadingCount()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertTrue(t.parse("You're assigned to kill 132 aberrant spectres; only 132 more to go."));
		assertEquals("aberrant spectres", t.getCurrentTask());
	}

	@Test
	public void parsesCurrentlyAssignedCheck()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertTrue(t.parse("You're currently assigned to kill gargoyles; only 122 more to go."));
		assertEquals("gargoyles", t.getCurrentTask());
	}

	@Test
	public void stripsKonarLocationSuffix()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertTrue(t.parse("You are assigned to kill abyssal demons in the Catacombs of Kourend; only 145 more to go."));
		assertEquals("abyssal demons", t.getCurrentTask());
	}

	@Test
	public void ignoresUnrelatedMessages()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertFalse(t.parse("Oh dear, you are dead!"));
		assertNull(t.getCurrentTask());
	}

	@Test
	public void clearsOnCompletion()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		t.parse("You are assigned to kill hellhounds; only 5 more to go.");
		assertEquals("hellhounds", t.getCurrentTask());
		assertTrue(t.parse("You have completed your task! You killed 50 hellhounds."));
		assertNull(t.getCurrentTask());
	}

	@Test
	public void noDuplicateChangeForSameTask()
	{
		SlayerTaskTracker t = new SlayerTaskTracker();
		assertTrue(t.parse("You're currently assigned to kill nechryael; only 80 more to go."));
		assertFalse(t.parse("You're currently assigned to kill nechryael; only 79 more to go."));
	}
}
