package com.example.antfarm.colony;

/**
 * Adult ant caste. The colony context owns the caste vocabulary because it
 * decides which role a hatchling takes (round-robin balance) and the queen
 * is colony-internal; the ants context maps roles to behaviour.
 */
public enum Role {
	WORKER,
	FORAGER
}
