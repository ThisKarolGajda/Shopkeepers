package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class TextValue extends ValueType<@NonNull Text> {

	public static final TextValue INSTANCE = new TextValue();

	private static final ColoredStringValue COLORED_STRING_VALUE = new ColoredStringValue();

	public TextValue() {
	}

	@Override
	public @Nullable Text load(@Nullable Object configValue) throws ValueLoadException {
		String string = COLORED_STRING_VALUE.load(configValue);
		if (string == null) return null;
		return Text.parse(string);
	}

	@Override
	public @Nullable Object save(@Nullable Text value) {
		if (value == null) return null;
		return COLORED_STRING_VALUE.save(value.toPlainFormatText());
	}

	@Override
	public String format(@Nullable Text value) {
		if (value == null) return "null";
		return TextUtils.decolorize(value.toPlainFormatText());
	}

	@Override
	public Text parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		return Text.parse(input);
	}
}
