# Motherlode Mine Mining Markers
![MLM Markers in Action](mlmmarkers.png)

This plugin helps you remember which veins you've mined in
the Motherlode Mine as well as keep track
of veins mined by other players.

While not perfect, this will help you identify veins that only
have one ore left in them. This is not a marker to tell you to
avoid them though! Do yourself and everyone around you a favor
by cleaning these leftover veins and help make MLM just a bit
more chill.

For details about the mechanics of the upper level veins in the Motherlode Mine, please visit the
[Old School Runescape Wiki](https://oldschool.runescape.wiki/w/Motherlode_Mine#Upper_level).

## Update

As of [2022-07-13](https://secure.runescape.com/m=news/emirs-arena-full-launch?oldschool=1),
Upper Level Motherlode ore veins have their respawn timer start
from the ore timer ending rather than the ore being depleted!
This means this plugin is now slightly redundant, however a new
config setting, **Respawn Timeout**, has been added to reflect
this change.

By default, after two minutes of one of this plugin's markers
being active, it will remove itself if the rock has not been
fully mined yet, assuming the rock is about to respawn.

## Update 2

As of [2024-05-08](https://secure.runescape.com/m=news/project-rebalance-skilling--poll-81-mta-changes?oldschool=1),
both levels of the Motherlode Mine have their ore veins function on timers!
The plugin has been modified to reflect this change, allowing you to set
timeouts for both upper and lower level separately. The option to show only
the markers when on the upper level has been changed to either showing all markers
or only markers on the same level as yours.

## Known issues

* The plugin currently works by detecting the second time a player
  performs a mining animation on a given tile. This means that:
  * Mining two veins in a corner will cause the second vein to be
    marked on the first animation rather than the second.
  * Mining a new vein for a single pay-dirt will not be marked.
  * It is possible to force trigger the markers by simply starting
    to mine a vein and walking back, then mining the vein again.
  * The markers will still trigger if you perform a full mining
    animation cycle without mining a pay-dirt.
  * Sometimes, the mining animation can start before a player
    is on the actual tile they should on, meaning the vein will
    be marked late.
