## *Info*:

**Angle Warp** is a client-side mod that helps aiming at certain points in space, by introducing a rotation-based waypoint system. [[Video Demo](https://www.youtube.com/watch?v=TOV1VCN-nPs)]

The primary use-case of this mod is to help teleporting via [arrow deflection-based wireless redstone](https://www.youtube.com/watch?v=FnUE-ZaALLw).

## *Features*:
* In-game waypoint overlay (can be enabled by holding the R key)
* Automatic cursor snapping to the nearest waypoint
* Progress bar that tracks the activation progress
* Waypoint customization
* Waypoint linking (used for 2FA)


New waypoints can be added with this command:
```/anglewarp add_point <point_id> <yaw> <pitch> <warp_ticks>```<br>


## *2FA*:
To set up 2FA, first create a new point with the following command: ```/anglewarp add_point verification_point -56.01 -90.00```<br>
If you want to make this point hidden for view, activate this command: ```/anglewarp configure verification_point hide true```

You can then configure another point to use the 2FA point by entering:
```/anglewarp configure <point_id> 2fa verification_point```

<br><br><br>
This mod was created before I found out about [AngleSnap](https://github.com/KingContaria/anglesnap), the features of these two mods are more or less the same. However, there are things unique to this mod: the progress bar, automatic 2FA support, and the ability to hide/disable snapping on individual waypoints. AngleSnap has a GUI interface, while this mod is operated through client-side commands. This mod doesn't yet have custom icon support.
