package io.github.mahirsn.craftography.util;

/** Marks the client's copy of the overworld clock, so only its time is changed (see the clock mixins). */
public interface OverworldClock {
	void craftography$markOverworld();
}
