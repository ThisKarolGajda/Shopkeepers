package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public final class SKCitizensShopObjectType
		extends AbstractEntityShopObjectType<@NonNull SKCitizensShopObject>
		implements CitizensShopObjectType<@NonNull SKCitizensShopObject> {

	private final CitizensShops citizensShops;

	public SKCitizensShopObjectType(CitizensShops citizensShops) {
		super("citizen", Arrays.asList("npc"), "shopkeeper.citizen", SKCitizensShopObject.class);
		this.citizensShops = citizensShops;
	}

	@Override
	public boolean isEnabled() {
		return citizensShops.isEnabled();
	}

	@Override
	public String getDisplayName() {
		return Messages.shopObjectTypeNpc;
	}

	@Override
	public boolean mustBeSpawned() {
		return false; // Spawning and despawning is handled by Citizens.
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player creator,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		// A reduced set of checks compared to the default:
		if (spawnLocation == null || !spawnLocation.isWorldLoaded()) {
			if (creator != null) {
				TextUtils.sendMessage(creator, Messages.missingSpawnLocation);
			}
			return false;
		}
		return true;
	}

	@Override
	public SKCitizensShopObject createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		return new SKCitizensShopObject(citizensShops, shopkeeper, creationData);
	}
}
