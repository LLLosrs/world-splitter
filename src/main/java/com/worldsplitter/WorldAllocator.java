package com.worldsplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, side-effect free helpers that decide which worlds belong to which person.
 * <p>
 * Both allocation strategies guarantee two things the user asked for:
 * <ul>
 *     <li>every person's worlds are a single consecutive run from the sorted pool
 *     (never e.g. 302 then 360 for the same person)</li>
 *     <li>every world in the pool ends up assigned to exactly one person, with no gaps,
 *     no matter how the group size changes</li>
 * </ul>
 */
final class WorldAllocator
{
	private WorldAllocator()
	{
	}

	/**
	 * Fixed-size allocation used in "solo" mode: each person gets exactly {@code worldsPerPerson}
	 * consecutive worlds from the sorted pool. If the pool is smaller than
	 * {@code worldsPerPerson * totalPeople} the last people may receive fewer worlds (or none).
	 *
	 * @param sortedPool     ascending, deduplicated list of eligible world numbers
	 * @param totalPeople    how many people are splitting the pool
	 * @param worldsPerPerson how many worlds each person should receive
	 * @param position1Based which person we want the block for, 1 = first
	 * @return the consecutive list of worlds assigned to that person (never null, may be empty)
	 */
	static List<Integer> allocateFixedSize(
		List<Integer> sortedPool,
		int totalPeople,
		int worldsPerPerson,
		int position1Based)
	{
		if (sortedPool.isEmpty() || totalPeople < 1 || worldsPerPerson < 1
			|| position1Based < 1 || position1Based > totalPeople)
		{
			return new ArrayList<>();
		}

		int startIndex = (position1Based - 1) * worldsPerPerson;
		if (startIndex >= sortedPool.size())
		{
			return new ArrayList<>();
		}

		int endIndexExclusive = Math.min(startIndex + worldsPerPerson, sortedPool.size());
		return new ArrayList<>(sortedPool.subList(startIndex, endIndexExclusive));
	}

	/**
	 * Even allocation used in "group" mode: the whole pool is divided as evenly as possible
	 * across {@code totalPeople}, with any remainder worlds handed one-each to the first
	 * few people, so that the entire pool stays covered whenever the group grows or shrinks.
	 *
	 * @param sortedPool     ascending, deduplicated list of eligible world numbers
	 * @param totalPeople    current number of people in the group
	 * @param index0Based    this person's zero-based index/order within the group
	 * @return the consecutive list of worlds assigned to that person (never null, may be empty)
	 */
	static List<Integer> allocateEven(List<Integer> sortedPool, int totalPeople, int index0Based)
	{
		if (sortedPool.isEmpty() || totalPeople < 1 || index0Based < 0 || index0Based >= totalPeople)
		{
			return new ArrayList<>();
		}

		int poolSize = sortedPool.size();
		int base = poolSize / totalPeople;
		int remainder = poolSize % totalPeople;

		int startIndex = index0Based * base + Math.min(index0Based, remainder);
		int mySize = base + (index0Based < remainder ? 1 : 0);
		int endIndexExclusive = Math.min(startIndex + mySize, poolSize);

		if (startIndex >= poolSize)
		{
			return new ArrayList<>();
		}

		return new ArrayList<>(sortedPool.subList(startIndex, endIndexExclusive));
	}
}
