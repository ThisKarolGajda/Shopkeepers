package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.ShopkeeperUIHandler;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIConfig;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class EditorHandler extends AbstractEditorHandler implements ShopkeeperUIHandler {

	private static final ConfirmationUIConfig CONFIRMATION_UI_CONFIG_DELETE_SHOP = new ConfirmationUIConfig() {

		@Override
		public String getTitle() {
			return Messages.confirmationUiDeleteShopTitle;
		}

		@Override
		public @Nullable List<? extends @NonNull String> getConfirmationLore() {
			return Messages.confirmationUiDeleteShopConfirmLore;
		}
	};

	private final AbstractShopkeeper shopkeeper;

	protected EditorHandler(
			AbstractUIType uiType,
			AbstractShopkeeper shopkeeper,
			TradingRecipesAdapter tradingRecipesAdapter
	) {
		super(uiType, tradingRecipesAdapter);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	protected String getEditorTitle() {
		return Messages.editorTitle;
	}

	// EDITOR BUTTONS

	@Override
	protected ItemStack createTradeSetupIcon() {
		ShopType<?> shopType = this.getShopkeeper().getType();
		String itemName = StringUtils.replaceArguments(Messages.tradeSetupDescHeader,
				"shopType", shopType.getDisplayName()
		);
		List<? extends @NonNull String> itemLore = shopType.getTradeSetupDescription();
		return ItemUtils.setDisplayNameAndLore(
				Settings.tradeSetupItem.createItemStack(),
				itemName,
				itemLore
		);
	}

	@Override
	protected final void setupButtons() {
		super.setupButtons();
		this.setupShopkeeperButtons();
		this.setupShopObjectButtons();
	}

	protected void setupShopkeeperButtons() {
		this.addButtonOrIgnore(this.createDeleteButton());
		this.addButtonOrIgnore(this.createNamingButton());
	}

	protected void setupShopObjectButtons() {
		this.addButtons(shopkeeper.getShopObject().createEditorButtons());
	}

	protected Button createDeleteButton() {
		return new ActionButton(true) {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return DerivedSettings.deleteButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				editorSession.getUISession().closeDelayedAndRunTask(() -> {
					requestConfirmationDeleteShop(editorSession.getPlayer());
				});
				return true;
			}
		};
	}

	private void requestConfirmationDeleteShop(Player player) {
		ConfirmationUI.requestConfirmation(player, CONFIRMATION_UI_CONFIG_DELETE_SHOP, () -> {
			// Delete confirmed.
			if (!player.isValid()) return;
			if (!shopkeeper.isValid()) {
				// The shopkeeper has already been removed in the meantime.
				TextUtils.sendMessage(player, Messages.shopAlreadyRemoved);
				return;
			}

			// Call event:
			PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(
					shopkeeper,
					player
			);
			Bukkit.getPluginManager().callEvent(deleteEvent);
			if (!deleteEvent.isCancelled()) {
				// Delete the shopkeeper and save:
				shopkeeper.delete(player);
				shopkeeper.save();

				TextUtils.sendMessage(player, Messages.shopRemoved);
			}
			// Else: Cancelled by another plugin.
			// Note: We don't send a message in this case here, because we expect that the other
			// plugin sends a more specific message anyways if it wants to inform the player.
		}, () -> {
			// Delete cancelled.
			if (!player.isValid()) return;
			if (!shopkeeper.isValid()) return;

			// Try to open the editor again:
			// Note: This may currently not remember the previous editor state (such as the selected
			// trades page).
			SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(
					this.getUIType(),
					shopkeeper,
					player
			);
		});
	}

	protected @Nullable Button createNamingButton() {
		boolean useNamingButton = true;
		if (shopkeeper.getType() instanceof PlayerShopType) {
			// Naming via button enabled?
			if (Settings.namingOfPlayerShopsViaItem) {
				useNamingButton = false;
			} else {
				// No naming button for Citizens player shops if renaming is disabled for those.
				// TODO Restructure this to allow for dynamic editor buttons depending on shop
				// (object) types and settings.
				if (!Settings.allowRenamingOfPlayerNpcShops
						&& shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
					useNamingButton = false;
				}
			}
		}
		if (!useNamingButton) return null;

		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return DerivedSettings.nameButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				// Also triggers save:
				editorSession.getUISession().closeDelayed();

				// Start naming:
				Player player = editorSession.getPlayer();
				SKShopkeepersPlugin.getInstance().getShopkeeperNaming().startNaming(
						player,
						shopkeeper
				);
				TextUtils.sendMessage(player, Messages.typeNewName);
				return true;
			}
		};
	}

	@Override
	protected void saveRecipes(EditorSession editorSession) {
		// UI sessions are aborted (i.e. not saved) when the shopkeeper is removed:
		assert shopkeeper.isValid();
		Player player = editorSession.getPlayer();
		int changedOffers = tradingRecipesAdapter.updateTradingRecipes(
				player,
				editorSession.getRecipes()
		);
		if (changedOffers == 0) {
			Log.debug(() -> shopkeeper.getLogPrefix() + "No offers have changed.");
		} else {
			Log.debug(() -> shopkeeper.getLogPrefix() + changedOffers + " offers have changed.");

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// TODO Close all other UI sessions for the shopkeeper (e.g. trading players)? Also send
			// a message to them.
		}

		// Even if no trades have changed, the shopkeeper might have been marked as dirty due to
		// other editor options. If this is the case, we trigger a save here. Otherwise, we omit the
		// save.
		if (shopkeeper.isDirty()) {
			shopkeeper.save();
		}
	}
}
