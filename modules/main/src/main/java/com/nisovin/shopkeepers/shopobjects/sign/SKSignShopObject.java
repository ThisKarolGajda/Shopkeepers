package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.compat.MC_1_17;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	private static final String DATA_KEY_SIGN_TYPE = "signType";
	public static final Property<@NonNull SignType> SIGN_TYPE = new BasicProperty<@NonNull SignType>()
			.dataKeyAccessor(DATA_KEY_SIGN_TYPE, EnumSerializers.lenient(SignType.class))
			.validator(value -> {
				Validate.isTrue(value.isSupported(),
						() -> "Unsupported sign type: '" + value.name() + "'.");
			})
			.defaultValue(SignType.OAK)
			.build();

	public static final Property<@NonNull Boolean> WALL_SIGN = new BasicProperty<@NonNull Boolean>()
			.dataKeyAccessor("wallSign", BooleanSerializers.LENIENT)
			.defaultValue(true)
			.build();

	public static final Property<@NonNull Boolean> GLOWING_TEXT = new BasicProperty<@NonNull Boolean>()
			.dataKeyAccessor("glowingText", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	static {
		// Register shopkeeper data migrations:

		// Migration from TreeSpecies to SignType.
		// TODO Remove this again at some point. Added in v2.10.0.
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"sign-type",
				MigrationPhase.ofShopObjectClass(SKSignShopObject.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				boolean migrated = false;
				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				String signTypeName = shopObjectData.getString(DATA_KEY_SIGN_TYPE);
				if ("GENERIC".equals(signTypeName)) {
					Log.warning(logPrefix + "Migrating sign type from '" + signTypeName + "' to '"
							+ SignType.OAK + "'.");
					shopObjectData.set(SIGN_TYPE, SignType.OAK);
					migrated = true;
				} else if ("REDWOOD".equals(signTypeName)) {
					Log.warning(logPrefix + "Migrating sign type from '" + signTypeName + "' to '"
							+ SignType.SPRUCE + "'.");
					shopObjectData.set(SIGN_TYPE, SignType.SPRUCE);
					migrated = true;
				}
				return migrated;
			}
		});

		// Migration from sign facing to shopkeeper yaw (pre v2.13.4):
		// TODO Remove this migration again at some point.
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"sign-facing-to-yaw",
				MigrationPhase.ofShopObjectClass(SKSignShopObject.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				boolean migrated = false;
				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				String signFacingName = shopObjectData.getString("signFacing");
				if (signFacingName != null) {
					BlockFace signFacing = BlockFace.SOUTH;
					try {
						signFacing = BlockFace.valueOf(signFacingName);
					} catch (IllegalArgumentException e) {
						Log.warning(logPrefix + "Could not parse sign facing '" + signFacingName
								+ "'. Falling back to SOUTH.");
					}

					// Validate the sign facing:
					if (!this.isValidSignFacing(shopObjectData, signFacing)) {
						Log.warning(logPrefix + "Invalid sign facing '" + signFacingName
								+ "'. Falling back to SOUTH.");
						signFacing = BlockFace.SOUTH;
					}

					float yaw = BlockFaceUtils.getYaw(signFacing);
					Log.warning(logPrefix + "Migrating sign facing '" + signFacing + "' to yaw "
							+ TextUtils.format(yaw));
					shopkeeperData.set(AbstractShopkeeper.YAW, yaw);
					migrated = true;
				}
				return migrated;
			}

			private boolean isValidSignFacing(
					ShopObjectData shopObjectData,
					BlockFace signFacing
			) throws InvalidDataException {
				Boolean wallSign = shopObjectData.getOrNullIfMissing(WALL_SIGN); // Can be null
				if (wallSign == null) return true; // Skip the validation
				if (wallSign) {
					return BlockFaceUtils.isWallSignFacing(signFacing);
				} else {
					return BlockFaceUtils.isSignPostFacing(signFacing);
				}
			}
		});
	}

	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_PERIOD_SECONDS + 1
	);
	private static final long RESPAWN_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(3);

	protected final SignShops signShops;

	private final PropertyValue<@NonNull SignType> signTypeProperty = new PropertyValue<>(SIGN_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applySignType)
			.build(properties);
	private final PropertyValue<@NonNull Boolean> wallSignProperty = new PropertyValue<>(WALL_SIGN)
			.onValueChanged(Unsafe.initialized(this)::respawn)
			.build(properties);
	private final PropertyValue<@NonNull Boolean> glowingTextProperty = new PropertyValue<>(GLOWING_TEXT)
			.onValueChanged(Unsafe.initialized(this)::applyGlowingText)
			.build(properties);

	private @Nullable Block block = null;
	private long lastFailedRespawnAttemptMillis = 0;

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkLimiter = new RateLimiter(
			CHECK_PERIOD_SECONDS,
			nextCheckingOffset.getAndIncrement()
	);

	protected SKSignShopObject(
			SignShops signShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(shopkeeper, creationData);
		this.signShops = signShops;
		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.UP) {
				// Sign post:
				wallSignProperty.setValue(false, Collections.emptySet()); // Not marking dirty
			} // Else: Wall sign (default).
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return signShops.getSignShopObjectType();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		signTypeProperty.load(shopObjectData);
		wallSignProperty.load(shopObjectData);
		glowingTextProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		signTypeProperty.save(shopObjectData);
		wallSignProperty.save(shopObjectData);
		glowingTextProperty.save(shopObjectData);
		// Note: The sign facing is not saved, but instead derived from the shopkeeper's yaw.
	}

	// ACTIVATION

	@Override
	public @Nullable Block getBlock() {
		return block;
	}

	public @Nullable Sign getSign() {
		if (!this.isActive()) return null;
		Block block = Unsafe.assertNonNull(this.block);
		assert ItemUtils.isSign(block.getType());
		return (Sign) block.getState();
	}

	@Override
	public boolean isActive() {
		Block block = this.getBlock();
		if (block == null) return false; // Not spawned
		// The shopkeeper is despawned on chunk unload:
		assert Unsafe.assertNonNull(shopkeeper.getChunkCoords()).isChunkLoaded();
		if (!ItemUtils.isSign(block.getType())) return false; // No longer a sign
		return true;
	}

	@Override
	public boolean spawn() {
		if (block != null) {
			return true; // Already spawned
		}

		Location signLocation = shopkeeper.getLocation();
		if (signLocation == null) {
			return false;
		}

		// If re-spawning fails due to the sign dropping for some reason (ex. attached block
		// missing) this could be abused (sign drop farming), therefore we limit the number of spawn
		// attempts:
		if (System.currentTimeMillis() - lastFailedRespawnAttemptMillis < RESPAWN_TIMEOUT_MILLIS) {
			Log.debug(() -> shopkeeper.getLocatedLogPrefix() + "Sign is on spawn cooldown.");
			return false;
		}

		// Place sign:
		// This replaces any currently existing block at that location.
		Block signBlock = signLocation.getBlock();
		BlockData blockData = this.createBlockData();
		assert blockData != null;

		// Cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(blockData, false); // Skip physics update
		// Cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// In case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			lastFailedRespawnAttemptMillis = System.currentTimeMillis();
			this.cleanUpBlock(signBlock);
			return false;
		}

		// Remember the block (indicates that this shop object has been spawned):
		this.block = signBlock;
		// Assign metadata for easy identification by other plugins:
		ShopkeeperMetadata.apply(block);

		// Setup sign:
		this.updateSign();

		// Inform about the object id change:
		this.onIdChanged();

		return true;
	}

	private BlockData createBlockData() {
		SignType signType = this.getSignType();
		boolean wallSign = this.isWallSign();
		Material signMaterial = getSignMaterial(signType, wallSign);
		assert ItemUtils.isSign(signMaterial);
		BlockData signData;
		if (wallSign) {
			// Wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(signMaterial);
			wallSignData.setFacing(this.getSignFacing());
			signData = wallSignData;
		} else {
			// Sign post:
			org.bukkit.block.data.type.Sign signPostData = Unsafe.castNonNull(
					Bukkit.createBlockData(signMaterial)
			);
			signPostData.setRotation(this.getSignFacing());
			signData = signPostData;
		}
		return signData;
	}

	private static Material getSignMaterial(SignType signType, boolean wallSign) {
		assert signType != null && signType.isSupported();
		Material signMaterial;
		if (wallSign) {
			signMaterial = signType.getWallSignMaterial();
		} else {
			signMaterial = signType.getSignMaterial();
		}
		return Unsafe.assertNonNull(signMaterial);
	}

	@Override
	public void despawn() {
		Block block = this.block;
		if (block == null) return;

		// Cleanup:
		this.cleanUpBlock(block);

		// Remove the sign:
		block.setType(Material.AIR, false);
		this.block = null;

		// Inform about the object id change:
		this.onIdChanged();
	}

	// Any clean up that needs to happen for the block.
	protected void cleanUpBlock(Block block) {
		assert block != null;
		// Remove the metadata again:
		ShopkeeperMetadata.remove(block);
	}

	private void updateSign() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		// Setup sign contents:
		if (shopkeeper instanceof PlayerShopkeeper) {
			this.setupPlayerShopSign(sign, (PlayerShopkeeper) shopkeeper);
		} else {
			assert shopkeeper instanceof AdminShopkeeper;
			this.setupAdminShopSign(sign, (AdminShopkeeper) shopkeeper);
		}

		// Glowing text:
		NMSManager.getProvider().setGlowingText(sign, this.isGlowingText());

		// Apply sign changes:
		sign.update(false, false);
	}

	protected void setupPlayerShopSign(Sign sign, PlayerShopkeeper playerShop) {
		Map<@NonNull String, @NonNull Object> arguments = new HashMap<>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(this.prepareName(playerShop.getName())));
		arguments.put("owner", playerShop.getOwnerName());  // Not null, can be empty

		sign.setLine(0, StringUtils.replaceArguments(Messages.playerSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.playerSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.playerSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.playerSignShopLine4, arguments));
	}

	protected void setupAdminShopSign(Sign sign, AdminShopkeeper adminShop) {
		Map<@NonNull String, @NonNull Object> arguments = new HashMap<>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(this.prepareName(adminShop.getName())));

		sign.setLine(0, StringUtils.replaceArguments(Messages.adminSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.adminSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.adminSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.adminSignShopLine4, arguments));
	}

	// TICKING

	@Override
	public void onTick() {
		super.onTick();
		if (!checkLimiter.request()) {
			return;
		}

		if (this.isSpawningScheduled()) {
			Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLogPrefix()
					+ "Spawning is scheduled. Skipping sign check.");
			return;
		}

		// Indicate ticking activity for visualization:
		this.indicateTickActivity();

		// This is only called for shopkeepers in active (i.e. loaded) chunks, and shopkeepers are
		// despawned on chunk unload:
		assert Unsafe.assertNonNull(shopkeeper.getChunkCoords()).isChunkLoaded();

		if (!this.isActive()) {
			Log.debug(() -> shopkeeper.getLocatedLogPrefix()
					+ "Sign is missing! Attempting respawn.");
			// Cleanup any previously spawned block, and then respawn:
			this.despawn();
			boolean success = this.spawn();
			if (!success) {
				Log.warning(shopkeeper.getLocatedLogPrefix() + "Sign could not be spawned!");
			}
			return;
		}
	}

	@Override
	public @Nullable Location getTickVisualizationParticleLocation() {
		Location location = this.getLocation();
		if (location == null) return null;
		if (this.isWallSign()) {
			// Return location at the block center:
			return location.add(0.5D, 0.5D, 0.5D);
		} else {
			// Return location above the sign post:
			return location.add(0.5D, 1.3D, 0.5D);
		}
	}

	// NAMING

	@Override
	public void setName(@Nullable String name) {
		// Sign blocks don't have a name (the sign contents are language file specific). However,
		// this method is usually called when the shopkeeper is renamed, which may require an update
		// of the sign contents.
		this.updateSign();
	}

	@Override
	public @Nullable String getName() {
		// Sign blocks don't have a name (the sign contents are language file specific):
		return null;
	}

	// PLAYER SHOP OWNER

	@Override
	public void onShopOwnerChanged() {
		// Update the sign:
		this.updateSign();
	}

	// EDITOR ACTIONS

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSignTypeEditorButton());
		if (MC_1_17.isAvailable()) {
			editorButtons.add(this.getGlowingTextEditorButton());
		}
		return editorButtons;
	}

	// WALL SIGN (vs sign post)

	public boolean isWallSign() {
		return wallSignProperty.getValue();
	}

	// SIGN FACING

	public BlockFace getSignFacing() {
		if (this.isWallSign()) {
			return BlockFaceUtils.getWallSignFacings().fromYaw(shopkeeper.getYaw());
		} else {
			return BlockFaceUtils.getSignPostFacings().fromYaw(shopkeeper.getYaw());
		}
	}

	// SIGN TYPE

	public SignType getSignType() {
		return signTypeProperty.getValue();
	}

	public void setSignType(SignType signType) {
		signTypeProperty.setValue(signType);
	}

	protected void applySignType() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		// Note: The different sign types are different materials. We need to capture the sign state
		// (e.g. sign contents), because they would otherwise be removed when changing the block's
		// type.
		BlockData blockData = this.createBlockData();
		sign.setBlockData(blockData); // Keeps sign data (e.g. text) the same
		sign.update(true, false); // Force: Material has changed, skip physics update.
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(
				EnumUtils.cycleEnumConstant(
						SignType.class,
						this.getSignType(),
						backwards,
						SignType.IS_SUPPORTED
				)
		);
	}

	private ItemStack getSignTypeEditorItem() {
		Material signMaterial = Unsafe.assertNonNull(this.getSignType().getSignMaterial());
		ItemStack iconItem = new ItemStack(signMaterial);
		return ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonSignVariant,
				Messages.buttonSignVariantLore
		);
	}

	private Button getSignTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getSignTypeEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleSignType(backwards);
				return true;
			}
		};
	}

	// GLOWING TEXT

	public boolean isGlowingText() {
		return glowingTextProperty.getValue();
	}

	public void setGlowingText(boolean glowing) {
		glowingTextProperty.setValue(glowing);
	}

	protected void applyGlowingText() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		NMSManager.getProvider().setGlowingText(sign, this.isGlowingText());
		// Sign block type is still the same (no force required), and we want to skip physics:
		sign.update(false, false);
	}

	public void cycleGlowingText(boolean backwards) {
		this.setGlowingText(!this.isGlowingText());
	}

	private ItemStack getGlowingTextEditorItem() {
		ItemStack iconItem;
		if (this.isGlowingText()) {
			Material iconType = Unsafe.assertNonNull(MC_1_17.GLOW_INK_SAC.orElse(Material.INK_SAC));
			iconItem = new ItemStack(iconType);
		} else {
			iconItem = new ItemStack(Material.INK_SAC);
		}
		return ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonSignGlowingText,
				Messages.buttonSignGlowingTextLore
		);
	}

	private Button getGlowingTextEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getGlowingTextEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleGlowingText(backwards);
				return true;
			}
		};
	}
}
