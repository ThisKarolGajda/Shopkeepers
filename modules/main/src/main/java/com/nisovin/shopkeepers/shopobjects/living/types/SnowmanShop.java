package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Snowman;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class SnowmanShop extends SKLivingShopObject<Snowman> {

	public static final Property<Boolean> PUMPKIN_HEAD = new BasicProperty<Boolean>()
			.dataKeyAccessor("pumpkinHead", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> pumpkinHeadProperty = new PropertyValue<>(PUMPKIN_HEAD)
			.onValueChanged(this::applyPumpkinHead)
			.build(properties);

	public SnowmanShop(	LivingShops livingShops, SKLivingShopObjectType<SnowmanShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		pumpkinHeadProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		pumpkinHeadProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyPumpkinHead();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getPumpkinHeadEditorButton());
		return editorButtons;
	}

	// PUMPKIN HEAD

	public boolean hasPumpkinHead() {
		return pumpkinHeadProperty.getValue();
	}

	public void setPumpkinHead(boolean pumpkinHead) {
		pumpkinHeadProperty.setValue(pumpkinHead);
	}

	public void cyclePumpkinHead() {
		this.setPumpkinHead(!this.hasPumpkinHead());
	}

	private void applyPumpkinHead() {
		Snowman entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setDerp(!this.hasPumpkinHead());
	}

	private ItemStack getPumpkinHeadEditorItem() {
		ItemStack iconItem = new ItemStack(this.hasPumpkinHead() ? Material.CARVED_PUMPKIN : Material.PUMPKIN);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSnowmanPumpkinHead, Messages.buttonSnowmanPumpkinHeadLore);
		return iconItem;
	}

	private Button getPumpkinHeadEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorSession editorSession) {
				return getPumpkinHeadEditorItem();
			}

			@Override
			protected boolean runAction(EditorSession editorSession, InventoryClickEvent clickEvent) {
				cyclePumpkinHead();
				return true;
			}
		};
	}
}
