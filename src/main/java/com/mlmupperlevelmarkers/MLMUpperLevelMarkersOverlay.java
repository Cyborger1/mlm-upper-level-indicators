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
package com.mlmupperlevelmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.TextComponent;

class MLMUpperLevelMarkersOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 2350;
	private static final int OFFSET_Z = -10;

	private final Client client;
	private final MLMUpperLevelMarkersPlugin plugin;
	private final MLMUpperLevelMarkersConfig config;

	private final TextComponent textComponent = new TextComponent();

	@Inject
	MLMUpperLevelMarkersOverlay(Client client, MLMUpperLevelMarkersPlugin plugin, MLMUpperLevelMarkersConfig config)
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
		if (!plugin.isInMLM())
		{
			return null;
		}

		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		final LocalPoint playerLocalPoint = localPlayer.getLocalLocation();

		if (config.showOnlyWhenUpstairs() && !plugin.isUpstairs(playerLocalPoint))
		{
			return null;
		}

		final Duration firstTimeout = Duration.ofSeconds(config.getFirstTimeout());
		final Duration secondTimeout = Duration.ofSeconds(config.getSecondTimeout());

		for (Map.Entry<WorldPoint, StateTimePair> entry : plugin.getOreVeinStateMap().entrySet())
		{
			final OreVeinState state = entry.getValue().getState();
			final Instant time = entry.getValue().getTime();
			final LocalPoint localPoint = LocalPoint.fromWorld(client, entry.getKey());

			if (localPoint == null)
			{
				continue;
			}

			Color color;
			switch (state)
			{
				case MinedBySelf:
					color = config.getSelfMarkerColor();
					break;
				case MinedByOther:
					color = config.showOtherMarkers() ? config.getOtherMarkerColor() : null;
					break;
				default:
					color = null;
					break;
			}

			if (color != null && playerLocalPoint.distanceTo(localPoint) <= MAX_DISTANCE)
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
				if (poly != null)
				{
					Instant now = Instant.now();
					Duration sinceTime = Duration.between(time, now);
					long t1 = firstTimeout.getSeconds();
					long t2 = secondTimeout.getSeconds();
					if (t1 >= 0 && sinceTime.compareTo(firstTimeout) >= 0)
					{
						color = color.darker();
					}
					if (t2 >= 0 && sinceTime.compareTo(secondTimeout) >= 0)
					{
						color = color.darker();
					}

					OverlayUtil.renderPolygon(graphics, poly, color);

					MarkerTimerMode timerMode = config.getMarkerTimerMode();
					if (timerMode != MarkerTimerMode.Off)
					{
						long mills;
						if (timerMode == MarkerTimerMode.Timeout && (t1 > 0 || t2 > 0))
						{
							mills = Duration.between(now, time.plusSeconds(Math.max(t1, t2))).toMillis();
						}
						else if (timerMode == MarkerTimerMode.Counter)
						{
							mills = sinceTime.toMillis();
						}
						else
						{
							// Will not print text
							mills = -1;
						}

						if (mills >= 0)
						{
							String label = String.format("%.1f", mills / 1000f);
							Point canvasTextLocation = Perspective.getCanvasTextLocation(
								client, graphics, localPoint, label, OFFSET_Z);
							if (canvasTextLocation != null)
							{
								textComponent.setText(label);
								textComponent.setColor(color);
								textComponent.setOutline(true);
								textComponent.setPosition(
									new java.awt.Point(canvasTextLocation.getX(), canvasTextLocation.getY()));
								textComponent.render(graphics);
							}
						}
					}
				}
			}
		}

		return null;
	}
}
