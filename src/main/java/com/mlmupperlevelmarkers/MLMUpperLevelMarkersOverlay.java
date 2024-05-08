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
import java.awt.geom.Path2D;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
		setPriority(config.higherRenderPriority());
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	public void setPriority(boolean higher)
	{
		setPriority(higher ? PRIORITY_MED : PRIORITY_LOW);
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
		final boolean playerUpstairs = plugin.isUpstairs(playerLocalPoint);
		final boolean checkSameLevel = config.showOnlyOnSameLevel();
		final ShowMarkerType markersToShow = config.getMarkersToShow();

		final Duration firstTimeoutUL = Duration.ofSeconds(config.getFirstTimeoutUL());
		final Duration secondTimeoutUL = Duration.ofSeconds(config.getSecondTimeoutUL());
		final Duration firstTimeoutLL = Duration.ofSeconds(config.getFirstTimeoutLL());
		final Duration secondTimeoutLL = Duration.ofSeconds(config.getSecondTimeoutLL());
		final Duration respawnTimeout = Duration.ofSeconds(config.getRespawnTimeout());

		final MarkerTimerMode timerMode = config.getMarkerTimerMode();
		final int offset = config.getMarkerTimerOffset();

		final TileMarkerType markerType = config.tileMarkerType();

		final Instant now = Instant.now();

		final DecimalFormat timerDecimalFormat = new DecimalFormat(config.showMarkerTimerDecimal() ? "0.0" : "0");
		timerDecimalFormat.setRoundingMode(RoundingMode.CEILING);

		for (Map.Entry<WorldPoint, StateTimePair> entry : plugin.getOreVeinStateMap().entrySet())
		{
			final OreVeinState state = entry.getValue().getState();
			final Instant time = entry.getValue().getTime();
			final Duration sinceTime = Duration.between(time, now);
			final LocalPoint localPoint = LocalPoint.fromWorld(client, entry.getKey());

			if (localPoint == null)
			{
				continue;
			}

			final boolean pointUpstairs = plugin.isUpstairs(localPoint);

			if (markersToShow == ShowMarkerType.UPPER && !pointUpstairs
				|| markersToShow == ShowMarkerType.LOWER && pointUpstairs
				|| checkSameLevel && playerUpstairs != pointUpstairs)
			{
				continue;
			}
			
			// Do not display anymore if "respawned"
			if (respawnTimeout.getSeconds() >= 0 && sinceTime.compareTo(respawnTimeout) >= 0)
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

			final Duration ft = pointUpstairs ? firstTimeoutUL : firstTimeoutLL;
			final Duration st = pointUpstairs ? secondTimeoutUL : secondTimeoutLL;

			if (color != null && playerLocalPoint.distanceTo(localPoint) <= MAX_DISTANCE)
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
				if (poly != null)
				{
					long t1 = ft.getSeconds();
					long t2 = st.getSeconds();
					if (t1 >= 0 && sinceTime.compareTo(ft) >= 0)
					{
						color = color.darker();
					}
					if (t2 >= 0 && sinceTime.compareTo(st) >= 0)
					{
						color = color.darker();
					}

					// Adjust to treat disable (-1) as a value of 0
					t1 = Math.max(t1, 0);
					t2 = Math.max(t2, 0);

					final long maxt = Math.max(t1, t2);
					final double timeLeftMax = Duration.between(now, time.plusSeconds(maxt)).toMillis() / 1000f;

					switch (markerType)
					{
						case CONTOUR_TIMER:
							if (timeLeftMax <= 0)
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
							break;
						case NORMAL:
							OverlayUtil.renderPolygon(graphics, poly, color);
					}

					Double secs = null;

					switch (timerMode)
					{
						case Timeout:
							if (timeLeftMax > 0)
							{
								secs = timeLeftMax;
							}
							break;
						case PersistentTimeout:
							secs = Math.max(0, timeLeftMax);
							break;
						case Counter:
							secs = sinceTime.toMillis() / 1000d;
							break;
					}

					if (secs != null)
					{
						String label = timerDecimalFormat.format(secs);
						Point canvasTextLocation = Perspective.getCanvasTextLocation(
							client, graphics, localPoint, label, offset);
						if (canvasTextLocation != null)
						{
							textComponent.setText(label);
							textComponent.setColor(color);
							textComponent.setOutline(config.showMarkerTimerOutline());
							textComponent.setPosition(
								new java.awt.Point(canvasTextLocation.getX(), canvasTextLocation.getY()));
							textComponent.render(graphics);
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

		final int numPoints = poly.npoints;
		final int interpolatedLine = (int) (numPoints * interpolate);
		final double actualInterpolate = (numPoints * interpolate) - interpolatedLine;

		Path2D p1 = new Path2D.Double();
		Path2D p2 = new Path2D.Double();

		p1.moveTo(poly.xpoints[0], poly.ypoints[0]);

		Path2D curP = p1;

		for (int i = 0; i < numPoints; i++)
		{
			final int j = (i + 1) % numPoints;
			final int x2 = poly.xpoints[j];
			final int y2 = poly.ypoints[j];

			if (i == interpolatedLine)
			{
				final int x1 = poly.xpoints[i];
				final int y1 = poly.ypoints[i];
				final double interX = lerp(x1, x2, actualInterpolate);
				final double interY = lerp(y1, y2, actualInterpolate);
				curP.lineTo(interX, interY);
				curP = p2;
				curP.moveTo(interX, interY);
			}

			curP.lineTo(x2, y2);
		}

		// So the lines don't jiggle around as they get interpolated
		final Object prevStrokeControl = graphics.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		final Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

		graphics.setColor(color);
		graphics.draw(p1);
		graphics.setColor(color2);
		graphics.draw(p2);

		graphics.setColor(new Color(0, 0, 0, 50));
		graphics.fill(poly);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStrokeControl);
		graphics.setStroke(originalStroke);
	}

	private static double lerp(double a, double b, double f)
	{
		return a + f * (b - a);
	}
}
