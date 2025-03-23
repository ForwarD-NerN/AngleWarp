Client-side fabric mod that is made to simplify interaction with [breeze-ball-based wireless redstone](https://www.youtube.com/watch?v=X7Ah-SJ0vBc).
You can check out the demo here: https://www.youtube.com/watch?v=TOV1VCN-nPs

## *How to use*:
1. New markers can be added with the following command:
```/anglewarp add_point <point_id> <yaw> <pitch> <warp_ticks>```<br>
2. Hold down the R key to reveal the markers.<br>
3. When you move your cursor close to a marker, it will snap to that marker, initiating the warping process. A progress bar will appear below the crosshair, indicating the remaining ticks for the warp.

## 2FA
To set up 2FA, first create a new point with the following command: ```/anglewarp add_point verification_point -56.01 -90.00```<br>
If you want to keep this point hidden from view, enter this command(optional): ```/anglewarp configure verification_point hide true```

You can then configure another point to require 2FA by entering:
```/anglewarp configure <point_id> 2fa verification_point```

