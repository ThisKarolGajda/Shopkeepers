package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.util.Utils;

public class BookPlayerShopType extends PlayerShopType<BookPlayerShopkeeper> {

	protected BookPlayerShopType() {
		super("book", ShopkeepersAPI.PLAYER_BOOK_PERMISSION);
	}

	@Override
	public BookPlayerShopkeeper loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.validateConfigSection(config);
		BookPlayerShopkeeper shopkeeper = new BookPlayerShopkeeper(config);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public BookPlayerShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.validateCreationData(creationData);
		BookPlayerShopkeeper shopkeeper = new BookPlayerShopkeeper((PlayerShopCreationData) creationData);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public String getCreatedMessage() {
		return Settings.msgBookShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("book");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedBookShop);
	}
}
