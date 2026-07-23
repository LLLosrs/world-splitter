package com.worldsplitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

public class WorldAllocatorTest
{
	private static List<Integer> pool(int start, int end)
	{
		return IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
	}

	@Test
	public void fixedSizeAllocationIsSequentialAndNonOverlapping()
	{
		List<Integer> pool = pool(301, 580); // 280 worlds

		List<Integer> person1 = WorldAllocator.allocateFixedSize(pool, 14, 20, 1);
		List<Integer> person2 = WorldAllocator.allocateFixedSize(pool, 14, 20, 2);

		assertEquals(20, person1.size());
		assertEquals(301, (int) person1.get(0));
		assertEquals(320, (int) person1.get(person1.size() - 1));

		assertEquals(20, person2.size());
		assertEquals(321, (int) person2.get(0));
		assertEquals(340, (int) person2.get(person2.size() - 1));
	}

	@Test
	public void fixedSizeAllocationReturnsEmptyPastEndOfPool()
	{
		List<Integer> pool = pool(301, 310); // only 10 worlds
		List<Integer> person2 = WorldAllocator.allocateFixedSize(pool, 5, 20, 2);
		assertTrue(person2.isEmpty());
	}

	@Test
	public void evenAllocationCoversEntirePoolWithNoGapsOrOverlapAcrossVariousGroupSizes()
	{
		List<Integer> pool = pool(301, 580); // 280 worlds

		for (int totalPeople : new int[] {1, 2, 3, 5, 7, 13, 20, 41})
		{
			List<Integer> merged = new ArrayList<>();
			for (int i = 0; i < totalPeople; i++)
			{
				merged.addAll(WorldAllocator.allocateEven(pool, totalPeople, i));
			}

			merged.sort(Integer::compareTo);
			assertEquals("totalPeople=" + totalPeople, pool, merged);

			Set<Integer> asSet = new HashSet<>(merged);
			assertEquals("no duplicates for totalPeople=" + totalPeople, pool.size(), asSet.size());
		}
	}

	@Test
	public void evenAllocationBlocksAreConsecutive()
	{
		List<Integer> pool = pool(301, 580);
		List<Integer> block = WorldAllocator.allocateEven(pool, 7, 3);

		for (int i = 1; i < block.size(); i++)
		{
			assertEquals((int) block.get(i - 1) + 1, (int) block.get(i));
		}
	}

	@Test
	public void reindexingAfterGroupShrinksStillCoversEverything()
	{
		List<Integer> pool = pool(301, 580);

		// 5 people, then person index 2 leaves -> group shrinks to 4, remaining members
		// are re-numbered 0..3 by the server; verify the new allocation is still complete.
		List<Integer> merged = new ArrayList<>();
		int[] remainingIndices = {0, 1, 2, 3}; // re-numbered after member 2 left
		for (int i : remainingIndices)
		{
			merged.addAll(WorldAllocator.allocateEven(pool, 4, i));
		}
		merged.sort(Integer::compareTo);
		assertEquals(pool, merged);
	}

	@Test
	public void fixedSizeAllocationRejectsPositionOutsideGroup()
	{
		List<Integer> pool = pool(301, 400);
		assertTrue(WorldAllocator.allocateFixedSize(pool, 2, 20, 3).isEmpty());
	}
}
