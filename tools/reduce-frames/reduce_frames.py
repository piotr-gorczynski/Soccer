import bpy
from math import inf

obj = bpy.context.object
if not (obj and obj.animation_data and obj.animation_data.action):
    print("⚠️ No active action on selected object.")
else:
    action = obj.animation_data.action
    scale_factor = 0.5  # halve duration each run (change if you want)

    # --- find current keyframe range ---
    kf_min = inf
    kf_max = -inf
    kf_count = 0
    for fc in action.fcurves:
        for k in fc.keyframe_points:
            x = float(k.co.x)
            kf_min = min(kf_min, x)
            kf_max = max(kf_max, x)
            kf_count += 1

    if kf_count == 0:
        print(f"⚠️ Action '{action.name}' has no keyframes.")
    else:
        before_start = int(round(kf_min))
        before_end   = int(round(kf_max))

        # --- scale all keyframe times ---
        moved = 0
        for fc in action.fcurves:
            for k in fc.keyframe_points:
                k.co.x *= scale_factor
                k.handle_left.x *= scale_factor
                k.handle_right.x *= scale_factor
                moved += 1

        # --- recompute range after scaling ---
        kf_min2 = inf
        kf_max2 = -inf
        for fc in action.fcurves:
            for k in fc.keyframe_points:
                x = float(k.co.x)
                kf_min2 = min(kf_min2, x)
                kf_max2 = max(kf_max2, x)

        after_start = int(round(kf_min2))
        after_end   = int(round(kf_max2))

        # Optionally sync the scene range to the new end
        bpy.context.scene.frame_start = min(1, after_start)  # keep 1 if start < 1
        bpy.context.scene.frame_end   = after_end

        print(f"✅ Scaled {moved} keyframes in '{action.name}' by {scale_factor:.2f} "
              f"({before_start}-{before_end} → {after_start}-{after_end})")
