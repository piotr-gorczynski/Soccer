import site, sys
site.addsitedir(site.getusersitepackages())
print("User site added:", site.getusersitepackages())
import bpy
import os
from PIL import Image

# === CONFIGURATION ===
# Folder where your rendered PNG frames are stored:
input_folder = r"C:\tmp"

# Output file path:
output_file = r"C:\tmp\spritesheet.png"

# Frame dimensions (match your Blender render size)
frame_width = 128
frame_height = 128

# Number of frames to merge
num_frames = 22
# ======================

# Collect frame filenames (only PNG)
files = sorted([
    f for f in os.listdir(input_folder)
    if f.lower().endswith(".png")
])[:num_frames]

if not files:
    raise RuntimeError("No PNG files found in the specified folder.")

# Open first image to check size
first = Image.open(os.path.join(input_folder, files[0])).convert("RGBA")

# Auto-detect width/height if not set
frame_width, frame_height = first.size

# Create empty sheet (width = frames × width, height = same)
sheet_width = frame_width * len(files)
sheet_height = frame_height
spritesheet = Image.new("RGBA", (sheet_width, sheet_height), (0, 0, 0, 0))

# Paste frames side by side
for i, fname in enumerate(files):
    path = os.path.join(input_folder, fname)
    frame = Image.open(path).convert("RGBA")
    x_offset = i * frame_width
    spritesheet.paste(frame, (x_offset, 0), frame)

# Save the final spritesheet
spritesheet.save(output_file, "PNG")
print(f"✅ Sprite sheet created: {output_file}")
