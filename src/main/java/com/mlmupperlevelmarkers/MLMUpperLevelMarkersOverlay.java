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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
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

		MarkerTimerMode timerMode = config.getMarkerTimerMode();
		int offset = config.getMarkerOffset();

		final boolean showContour = config.getShowContourTimer();

		final Instant now = Instant.now();

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
					final Duration sinceTime = Duration.between(time, now);
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

					// Adjust to treat disable (-1) as a value of 0
					t1 = Math.max(t1, 0);
					t2 = Math.max(t2, 0);

					final long maxt = Math.max(t1, t2);
					final double timeLeftMax = Duration.between(now, time.plusSeconds(maxt)).toMillis() / 1000f;
					if (timeLeftMax <= 0 || !showContour)
					{
						OverlayUtil.renderPolygon(graphics, poly, color);
					}
					else
					{
						final long mint = Math.min(t1, t2);
						final double timeLeftMin = Duration.between(now, time.plusSeconds(mint)).toMillis() / 1000f;

						double timeLeft;
						long target;
						if (timeLeftMin > 0)
						{
							timeLeft = timeLeftMin;
							target = mint;
						}
						else
						{
							timeLeft = timeLeftMax;
							target = maxt - mint;
						}
						renderTileWithMovingColor(graphics, poly, color, color.darker(), timeLeft / target);
					}

					if (timerMode != MarkerTimerMode.Off)
					{
						double secs;
						if (timerMode == MarkerTimerMode.Timeout && timeLeftMax > 0)
						{
							secs = timeLeftMax;
						}
						else if (timerMode == MarkerTimerMode.Counter)
						{
							secs = sinceTime.toMillis() / 1000f;
						}
						else
						{
							// Will not print text
							secs = -1;
						}

						if (secs >= 0)
						{
							String label = String.format("%.1f", secs);
							Point canvasTextLocation = Perspective.getCanvasTextLocation(
								client, graphics, localPoint, label, offset);
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

	public static void renderTileWithMovingColor(Graphics2D graphics, Polygon poly, Color color, Color color2, double interpolate)
	{
		if (interpolate <= 0)
		{
			OverlayUtil.renderPolygon(graphics, poly, color2);
			return;
		}
		else if (interpolate >= 1)
		{
			OverlayUtil.renderPolygon(graphics, poly, color);
			return;
		}

		final int npoints = poly.npoints;
		final int interpolatedLine = (int) (npoints * interpolate);
		final double actualInterpolate = (npoints * interpolate) - interpolatedLine;

		graphics.setColor(color);
		// So the lines don't jiggle around as they get interpolated
		final Object prevStroke = graphics.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		final Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2));

		for (int i = 0; i < npoints; i++)
		{
			final int j = (i + 1) % npoints;
			if (i == interpolatedLine)
			{
				double interX = lerp(poly.xpoints[i], poly.xpoints[j], actualInterpolate);
				double interY = lerp(poly.ypoints[i], poly.ypoints[j], actualInterpolate);
				graphics.draw(
					new Line2D.Double(
						poly.xpoints[i],
						poly.ypoints[i],
						interX,
						interY
					)
				);
				graphics.setColor(color2);
				graphics.draw(
					new Line2D.Double(
						interX,
						interY,
						poly.xpoints[j],
						poly.ypoints[j]
					)
				);
			}
			else
			{
				graphics.drawLine(
					poly.xpoints[i],
					poly.ypoints[i],
					poly.xpoints[j],
					poly.ypoints[j]
				);
			}
		}

		graphics.setColor(new Color(0, 0, 0, 50));
		graphics.fill(poly);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStroke);
		graphics.setStroke(originalStroke);
	}

	private static double lerp(double a, double b, double f)
	{
		return a + f * (b - a);
	}
}
