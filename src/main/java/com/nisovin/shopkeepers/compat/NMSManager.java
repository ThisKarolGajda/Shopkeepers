package com.nisovin.shopkeepers.compat;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.Log;

public final class NMSManager {

	private static NMSCallProvider provider;

	public static NMSCallProvider getProvider() {
		return NMSManager.provider;
	}

	public static void load(Plugin plugin) {
		String packageName = plugin.getServer().getClass().getPackage().getName();
		String cbversion = packageName.substring(packageName.lastIndexOf('.') + 1);
		try {
			Class<?> clazz = Class.forName("com.nisovin.shopkeepers.compat." + cbversion + ".NMSHandler");
			if (NMSCallProvider.class.isAssignableFrom(clazz)) {
				NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
			} else {
				throw new Exception("Nope");
			}
		} catch (Exception e) {
			Log.severe("Potentially incompatible server version: " + cbversion);
			Log.severe("Shopkeepers is trying to run in 'compatibility mode'.");
			Log.info("Check for updates at: " + plugin.getDescription().getWebsite());

			try {
				NMSManager.provider = new FailedHandler();
			} catch (Exception e_u) {
				// uncomment for debugging:
				// e_u.printStackTrace();
			}
		}
	}
}
