package com.nisovin.shopkeepers.api.shopkeeper.offers;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * Stores information about one or two item stacks being traded for another item stack.
 * <p>
 * Instances of this are immutable. They can be created via {@link #create(ItemStack, ItemStack, ItemStack)}.
 */
public interface TradeOffer {

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * If the given item stacks are {@link UnmodifiableItemStack}s, they are assumed to be immutable and therefore not
	 * copied before they are stored by the trade offer. Otherwise, they are first copied.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 */
	public static TradeOffer create(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return ShopkeepersAPI.createTradeOffer(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are stored by the trade
	 * offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 */
	public static TradeOffer create(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		return ShopkeepersAPI.createTradeOffer(resultItem, item1, item2);
	}

	// ----

	/**
	 * Gets the result item.
	 * 
	 * @return an unmodifiable view on the result item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getResultItem();

	/**
	 * Gets the first required item.
	 * 
	 * @return an unmodifiable view on the first required item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getItem1();

	/**
	 * Gets the second required item.
	 * 
	 * @return an unmodifiable view on the second required item, can be <code>null</code>
	 */
	public UnmodifiableItemStack getItem2();

	/**
	 * Checks whether this trade offer requires two input items.
	 * <p>
	 * This is a shortcut for checking if {@link #getItem2()} is not <code>null</code>.
	 * 
	 * @return <code>true</code> if this is a trade offer with two input items
	 */
	public boolean hasItem2();

	/**
	 * Checks if the given items are equal to the items of this trade offer.
	 * 
	 * @param resultItem
	 *            the result item
	 * @param item1
	 *            the first item
	 * @param item2
	 *            the second item
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(ItemStack resultItem, ItemStack item1, ItemStack item2);

	/**
	 * Checks if the given items are equal to the items of this trade offer.
	 * 
	 * @param resultItem
	 *            the result item
	 * @param item1
	 *            the first item
	 * @param item2
	 *            the second item
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2);

	/**
	 * Checks if the items of this offer and the given trading recipe are equal.
	 * 
	 * @param tradingRecipe
	 *            the trading recipe to compare with
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(TradingRecipe tradingRecipe);
}
