Client-side fabric mod that is made to simplify interaction with [breeze-ball-based wireless redstone](https://www.youtube.com/watch?v=X7Ah-SJ0vBc).
Demo: https://www.youtube.com/watch?v=TOV1VCN-nPs

### *How to use*:
New markers can be added with ```/anglewarp add_point <point_id> <yaw> <pitch> <warp_ticks>```<br>

If you hold down the R key, markers will appear at the set positions<br>
When the cursor is near a marker, it automatically snaps to it and begins the warping process. The progress bar is shown below the crosshair to keep track of the warp ticks.<br>

To add 2FA, you need to create a new point: ```/anglewarp add_point verification_point -56.01 -90.00```<br>
It can be optionally hidden with: ```/anglewarp configure verification_point hide true```

That point can then be set as 2FA for another one:
```/anglewarp configure <point_id> 2fa verification_point```

