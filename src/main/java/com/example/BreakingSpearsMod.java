package com.example;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreakingSpearsMod implements ModInitializer {
	public static final String MOD_ID = "breaking_spears";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Breaking Spears initialized");
	}
}
