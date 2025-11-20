# recolor_blue.py
from PIL import Image
import colorsys

# Path to input/output files
input_path = r"C:\tmp\spritesheet.png"
output_path = r"C:\tmp\spritesheet_blue.png"

# Load the spritesheet
img = Image.open(input_path).convert("RGBA")
pixels = img.load()

for y in range(img.height):
    for x in range(img.width):
        r, g, b, a = pixels[x, y]
        if a == 0:
            continue

        # Convert to HSV
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)

        # Detect red hues (approx. 0–20° or 340–360°)
        if (h < 0.06 or h > 0.94) and s > 0.4 and v > 0.2:
            # Shift hue toward blue (add ~0.6)
            h = (h + 0.6) % 1.0
            r, g, b = [int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v)]
            pixels[x, y] = (r, g, b, a)

img.save(output_path)
print(f"✅ Saved as {output_path}")
