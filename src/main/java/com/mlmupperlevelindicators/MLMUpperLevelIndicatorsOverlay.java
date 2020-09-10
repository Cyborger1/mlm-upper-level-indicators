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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class MLMUpperLevelIndicatorsOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 2350;

	private final Client client;
	private final MLMUpperLevelIndicatorsPlugin plugin;
	private final MLMUpperLevelIndicatorsConfig config;

	@Inject
	MLMUpperLevelIndicatorsOverlay(Client client, MLMUpperLevelIndicatorsPlugin plugin, MLMUpperLevelIndicatorsConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// TODO: Plug config here
		if (!plugin.isInMLM())
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		LocalPoint playerLocalPoint = localPlayer.getLocalLocation();

		for (Map.Entry<WorldPoint, OreVeinState> entry : plugin.getOreVeinStateMap().entrySet())
		{
			OreVeinState state = entry.getValue();
			LocalPoint localPoint = LocalPoint.fromWorld(client, entry.getKey());

			if (localPoint == null)
			{
				continue;
			}

			if (playerLocalPoint.distanceTo(localPoint) <= MAX_DISTANCE &&
				(state == OreVeinState.MinedBySelf || state == OreVeinState.MinedByOther))
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
				if (poly != null)
				{
					Color color = state == OreVeinState.MinedBySelf ? Color.GREEN : Color.YELLOW;

					OverlayUtil.renderPolygon(graphics, poly, color);
				}
			}
		}

		return null;
	}
}
