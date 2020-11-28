package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

public class PlayerUtils {

	private PlayerUtils() {
	}

	/*
	 * Assumptions:
	 * - Names are unique (including case) among online players.
	 * - Allowed characters for names: [a-zA-Z0-9_]
	 * - Names don't contain whitespace.
	 * - Names don't include color codes.
	 * - Display names may include whitespace, color codes, arbitrary characters and may not be unique.
	 */
	public interface PlayerNameMatcher extends ObjectMatcher<Player> {

		@Override
		public Stream<Player> match(String input);

		/**
		 * Whether this {@link PlayerNameMatcher} matches display names.
		 * 
		 * @return <code>true</code> if matching display names
		 */
		public boolean matchesDisplayNames();

		// COMMON NAME MATCHERS

		public static final PlayerNameMatcher NAME_EXACT = new PlayerNameMatcher() {
			@Override
			public Stream<Player> match(String input) {
				if (StringUtils.isEmpty(input)) return Stream.empty();
				// Note: This is case insensitive.
				// Assumption: Player names are unique regardless of case.
				Player exactMatch = Bukkit.getPlayerExact(input);
				return (exactMatch != null) ? Stream.of(exactMatch) : Stream.empty();
			}

			@Override
			public boolean matchesDisplayNames() {
				return false;
			}
		};

		// Includes matching display names.
		public static final PlayerNameMatcher EXACT = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean checkExactMatchFirst() {
				// We check for exact matches later anyways so we can avoid this.
				return false;
			}

			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.equals(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.getPlayer(String) but also considers display names and ignores
		// dashes/underscores/whitespace.
		public static final PlayerNameMatcher STARTS_WITH = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.startsWith(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.matchPlayer(String) but also considers display names and ignores
		// dashes/underscores/whitespace.
		public static final PlayerNameMatcher CONTAINS = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.contains(normalizedInputName);
			}
		};
	}

	private static abstract class AbstractPlayerNameMatcher implements PlayerNameMatcher {

		@Override
		public Stream<Player> match(String input) {
			if (StringUtils.isEmpty(input)) return Stream.empty();

			// Check for an exact match first:
			if (this.checkExactMatchFirst()) {
				Player exactMatch = Bukkit.getPlayerExact(input); // Case insensitive
				if (exactMatch != null) return Stream.of(exactMatch);
			}

			String normalizedInput = StringUtils.normalize(input);
			List<Player> matchingPlayers = new ArrayList<>();
			boolean[] onlyPerfectMatches = new boolean[] { false };
			for (Player player : Bukkit.getOnlinePlayers()) {
				// Check name:
				String playerName = player.getName();
				String normalizedPlayerName = StringUtils.normalize(playerName);

				boolean matched = this.match(normalizedInput, player, normalizedPlayerName, matchingPlayers, onlyPerfectMatches);
				if (matched) {
					if (onlyPerfectMatches[0]) {
						// We found an exact player name match, return that player:
						// Note: This can usually only occur with checkExactMatchFirst disabled.
						return Stream.of(player);
					}
					continue; // Add player at most once. -> Skip display name check.
				}

				// Check display name:
				String displayName = player.getDisplayName();
				String normalizedDisplayName = StringUtils.normalize(TextUtils.stripColor(displayName));
				this.match(normalizedInput, player, normalizedDisplayName, matchingPlayers, onlyPerfectMatches);
			}
			return matchingPlayers.stream();
		}

		@Override
		public boolean matchesDisplayNames() {
			return true;
		}

		protected boolean checkExactMatchFirst() {
			return true;
		}

		protected boolean match(String normalizedInput, Player player, String normalizedName,
								List<Player> matchingPlayers, boolean[] onlyPerfectMatches) {
			if (this.matches(normalizedInput, normalizedName)) {
				if (normalizedName.length() == normalizedInput.length()) {
					// Perfect match of normalized names:
					if (!onlyPerfectMatches[0]) {
						// The previous matches were not perfect matches, disregard them:
						matchingPlayers.clear();
					}
					onlyPerfectMatches[0] = true; // Only accepting other perfect matches now
					matchingPlayers.add(player);
					return true;
				} else {
					if (!onlyPerfectMatches[0]) {
						matchingPlayers.add(player);
						return true;
					} // Else: Only accepting perfect matches.
				}
			}
			return false; // No match
		}

		protected abstract boolean matches(String normalizedInputName, String normalizedName);
	}

	private static final int DEFAULT_AMBIGUOUS_PLAYER_NAME_MAX_ENTRIES = 5;

	// Note: Iterable is only iterated once.
	// true if there are multiple matches.
	public static boolean handleAmbiguousPlayerName(CommandSender sender, String name, Iterable<Map.Entry<UUID, String>> matches) {
		return handleAmbiguousPlayerName(sender, name, matches, DEFAULT_AMBIGUOUS_PLAYER_NAME_MAX_ENTRIES);
	}

	// Note: Iterable is only iterated once.
	// true if there are multiple matches.
	public static boolean handleAmbiguousPlayerName(CommandSender sender, String name, Iterable<Map.Entry<UUID, String>> matches, int maxEntries) {
		return CommandUtils.handleAmbiguousInput(sender, name, matches, maxEntries,
				() -> {
					TextUtils.sendMessage(sender, Messages.ambiguousPlayerName, "name", name);
				},
				(match, index) -> {
					UUID matchUUID = match.getKey();
					String matchUUIDString = matchUUID.toString();
					String matchName = match.getValue();

					TextUtils.sendMessage(sender, Messages.ambiguousPlayerNameEntry,
							"index", index,
							"name", Text.insertion(matchName).childText(matchName).buildRoot(),
							"uuid", Text.insertion(matchUUIDString).childText(matchUUIDString).buildRoot()
					);
				},
				() -> {
					TextUtils.sendMessage(sender, Messages.ambiguousPlayerNameMore);
				}
		);
	}
}
