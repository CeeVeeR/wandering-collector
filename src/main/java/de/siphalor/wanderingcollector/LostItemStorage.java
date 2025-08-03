

package de.siphalor.wanderingcollector;

import com.mojang.serialization.Codec;

import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class LostItemStorage {
	private static final Random RANDOM = new Random();
	private final List<ItemStack> stacks = new ArrayList<>();

	// Hardcoded config values
	private static final boolean COMBINE_LOST_STACKS = true;
	private static final int MAX_LOST_STACK_AMOUNT = 64;
	private static final PollMode OFFER_CREATION = PollMode.RANDOM;

	// Codec for serialization
	public static final Codec<List<ItemStack>> STACKS_CODEC = ItemStack.CODEC.listOf();

	public void read(ReadView view) {
		stacks.clear();

		view.read(WanderingCollector.LOST_STACKS_KEY, STACKS_CODEC)
				.ifPresent(stacks::addAll);
	}

	public void write(WriteView view) {
		if (!stacks.isEmpty()) {
			view.put(WanderingCollector.LOST_STACKS_KEY, STACKS_CODEC, stacks);
		}
	}

	public boolean isEmpty() {
		return stacks.isEmpty();
	}

	public void add(ItemStack newStack) {
		if (COMBINE_LOST_STACKS && tryCombine(newStack)) {
			return;
		}

		if (stacks.size() >= MAX_LOST_STACK_AMOUNT) {
			stacks.removeFirst();
		}
		stacks.add(newStack);
	}

	private boolean tryCombine(ItemStack newStack) {
		int requiredSpace = newStack.getCount();
		int space = 0;
		List<ItemStack> equalStacks = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (ItemStack.areItemsAndComponentsEqual(stack, newStack)) {
				equalStacks.add(stack);
				space += stack.getMaxCount() - stack.getCount();

				if (space >= requiredSpace) {
					break;
				}
			}
		}

		if (space < requiredSpace) {
			return false;
		}

		for (ItemStack equalStack : equalStacks) {
			int move = Math.min(equalStack.getMaxCount() - equalStack.getCount(), requiredSpace);
			equalStack.increment(move);
			requiredSpace -= move;
			if (requiredSpace <= 0) {
				break;
			}
		}
		return true;
	}

	public Collection<ItemStack> poll(int stackCount) {
		stackCount = Math.min(stackCount, stacks.size());
		List<ItemStack> result = new ArrayList<>(stackCount);
		switch (OFFER_CREATION) {
			case NEWEST: {
				List<ItemStack> range = stacks.subList(stacks.size() - stackCount, stacks.size());
				result.addAll(range);
				range.clear();
				break;
			}
			case OLDEST: {
				List<ItemStack> range = stacks.subList(0, stackCount);
				result.addAll(range);
				range.clear();
				break;
			}
			case RANDOM:
				for (int i = 0; i < stackCount; i++) {
					int randIndex = RANDOM.nextInt(stacks.size());
					ItemStack stack = stacks.get(randIndex);
					if (stack.getCount() > stack.getMaxCount()) {
						result.add(stack.split(stack.getMaxCount()));
					} else {
						result.add(stack);
						stacks.remove(randIndex);
					}
				}
				break;
		}
		return result;
	}

	public enum PollMode {NEWEST, OLDEST, RANDOM}
}