package com.nisovin.shopkeepers.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIRegistry;

public interface ShopkeepersPlugin extends Plugin {

	public static ShopkeepersPlugin getInstance() {
		return ShopkeepersAPI.getPlugin();
	}

	// PERMISSIONS

	public static final String HELP_PERMISSION = "shopkeeper.help";
	public static final String TRADE_PERMISSION = "shopkeeper.trade";
	public static final String RELOAD_PERMISSION = "shopkeeper.reload";
	public static final String DEBUG_PERMISSION = "shopkeeper.debug";
	public static final String CLEANUP_CITIZEN_SHOPKEEPERS = "shopkeeper.cleanup-citizen-shopkeepers";

	public static final String LIST_OWN_PERMISSION = "shopkeeper.list.own";
	public static final String LIST_OTHERS_PERMISSION = "shopkeeper.list.others";
	public static final String LIST_ADMIN_PERMISSION = "shopkeeper.list.admin";

	public static final String REMOVE_ALL_OWN_PERMISSION = "shopkeeper.remove-all.own";
	public static final String REMOVE_ALL_OTHERS_PERMISSION = "shopkeeper.remove-all.others";
	public static final String REMOVE_ALL_PLAYER_PERMISSION = "shopkeeper.remove-all.player";
	public static final String REMOVE_ALL_ADMIN_PERMISSION = "shopkeeper.remove-all.admin";

	public static final String GIVE_PERMISSION = "shopkeeper.give";
	public static final String GIVE_CURRENCY_PERMISSION = "shopkeeper.givecurrency";
	public static final String CONVERT_ITEMS_OWN_PERMISSION = "shopkeeper.convertitems.own";
	public static final String CONVERT_ITEMS_OTHERS_PERMISSION = "shopkeeper.convertitems.others";
	public static final String REMOTE_PERMISSION = "shopkeeper.remote";
	public static final String REMOTE_OTHER_PLAYERS_PERMISSION = "shopkeeper.remote.otherplayers";
	public static final String REMOTE_EDIT_PERMISSION = "shopkeeper.remoteedit";
	public static final String TRANSFER_PERMISSION = "shopkeeper.transfer";
	public static final String SET_TRADE_PERM_PERMISSION = "shopkeeper.settradeperm";
	public static final String SET_FOR_HIRE_PERMISSION = "shopkeeper.setforhire";
	public static final String HIRE_PERMISSION = "shopkeeper.hire";
	public static final String EDIT_VILLAGERS_PERMISSION = "shopkeeper.edit-villagers";
	public static final String EDIT_WANDERING_TRADERS_PERMISSION = "shopkeeper.edit-wandering-traders";
	public static final String BYPASS_PERMISSION = "shopkeeper.bypass";
	public static final String MAXSHOPS_UNLIMITED_PERMISSION = "shopkeeper.maxshops.unlimited";

	public static final String ADMIN_PERMISSION = "shopkeeper.admin";
	public static final String PLAYER_SELL_PERMISSION = "shopkeeper.player.sell";
	public static final String PLAYER_BUY_PERMISSION = "shopkeeper.player.buy";
	public static final String PLAYER_TRADE_PERMISSION = "shopkeeper.player.trade";
	public static final String PLAYER_BOOK_PERMISSION = "shopkeeper.player.book";

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public boolean hasCreatePermission(Player player);

	// SHOP TYPES

	public ShopTypesRegistry<?> getShopTypeRegistry();

	public DefaultShopTypes getDefaultShopTypes();

	// SHOP OBJECT TYPES

	public ShopObjectTypesRegistry<?> getShopObjectTypeRegistry();

	public DefaultShopObjectTypes getDefaultShopObjectTypes();

	// UI

	public UIRegistry<?> getUIRegistry();

	public DefaultUITypes getDefaultUITypes();

	// SHOPKEEPER REGISTRY

	public ShopkeeperRegistry getShopkeeperRegistry();

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public ShopkeeperStorage getShopkeeperStorage();

	//

	/**
	 * Creates and spawns a new shopkeeper in the same way a player would create it.
	 * <p>
	 * This takes any limitations into account that might affect the creator of the shopkeeper, and this sends the
	 * creator messages if the shopkeeper creation fails for some reason.
	 * <p>
	 * This requires a {@link ShopCreationData} with non-<code>null</code> creator.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments for creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public Shopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData);

	// OFFERS FACTORY

	public PriceOffer createPriceOffer(ItemStack item, int price);

	public TradeOffer createTradeOffer(ItemStack resultItem, ItemStack item1, ItemStack item2);

	public BookOffer createBookOffer(String bookTitle, int price);
}
