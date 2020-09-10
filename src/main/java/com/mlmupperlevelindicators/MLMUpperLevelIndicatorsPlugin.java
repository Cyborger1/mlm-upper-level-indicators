/*
 * Copyright (c) 2020, Cyborger1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.mlmupperlevelindicators;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static net.runelite.api.AnimationID.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import static net.runelite.api.ObjectID.ORE_VEIN_26661;
import static net.runelite.api.ObjectID.ORE_VEIN_26662;
import static net.runelite.api.ObjectID.ORE_VEIN_26663;
import static net.runelite.api.ObjectID.ORE_VEIN_26664;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "MLM Upper Level Indicators",
	description = "Helps you remember to go back and clean up your one-rocks!",
	tags = {"Motherlode", "Mine", "Indicator", "MLM"}
)
public class MLMUpperLevelIndicatorsPlugin extends Plugin
{
	// From official Motherlode plugin
	private static final Set<Integer> MOTHERLODE_MAP_REGIONS = ImmutableSet.of(14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193);
	private static final Set<Integer> MINE_SPOTS = ImmutableSet.of(ORE_VEIN_26661, ORE_VEIN_26662, ORE_VEIN_26663, ORE_VEIN_26664);
	private static final Set<Integer> MINING_ANIMATION_IDS = ImmutableSet.of(
		MINING_MOTHERLODE_BRONZE, MINING_MOTHERLODE_IRON, MINING_MOTHERLODE_STEEL,
		MINING_MOTHERLODE_BLACK, MINING_MOTHERLODE_MITHRIL, MINING_MOTHERLODE_ADAMANT,
		MINING_MOTHERLODE_RUNE, MINING_MOTHERLODE_GILDED, MINING_MOTHERLODE_DRAGON,
		MINING_MOTHERLODE_DRAGON_UPGRADED, MINING_MOTHERLODE_DRAGON_OR, MINING_MOTHERLODE_INFERNAL,
		MINING_MOTHERLODE_CRYSTAL
	);

	public enum OreVeinState
	{
		Untouched,
		MinedBySelf,
		MinedByOther
	}

	@Inject
	private Client client;

	@Inject
	private MLMUpperLevelIndicatorsConfig config;

	@Getter(AccessLevel.PACKAGE)
	private boolean inMLM;

	@Getter(AccessLevel.PACKAGE)
	private final Map<WorldPoint, OreVeinState> oreVeinStateMap = new HashMap<>();

	@Provides
	MLMUpperLevelIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MLMUpperLevelIndicatorsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		// TODO: Overlays
		inMLM = checkInMLM();
	}

	@Override
	protected void shutDown() throws Exception
	{
		// TODO: Overlays
		oreVeinStateMap.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				oreVeinStateMap.clear();
				inMLM = checkInMLM();
				break;
			case LOGIN_SCREEN:
				inMLM = false;
				break;
		}
	}

	// From official Motherlode plugin
	private boolean checkInMLM()
	{
		GameState gameState = client.getGameState();
		if (gameState != GameState.LOGGED_IN
			&& gameState != GameState.LOADING)
		{
			return false;
		}

		// Verify that all regions exist in MOTHERLODE_MAP_REGIONS
		for (int region : client.getMapRegions())
		{
			if (!MOTHERLODE_MAP_REGIONS.contains(region))
			{
				return false;
			}
		}

		return true;
	}
}
