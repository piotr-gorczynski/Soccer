# Sprite Sheet Metadata Implementation

## Overview

This implementation adds a metadata-driven system for managing sprite sheet animations in the Soccer game. The system makes it easy to configure sprite animations without hardcoding frame dimensions, counts, and other properties.

## Components

### 1. SpriteSheetMetadata
A metadata class that describes sprite sheet properties using the Builder pattern.

**Properties:**
- `resourceId` - Android resource ID of the sprite sheet
- `frameWidth` - Width of each frame in pixels
- `frameHeight` - Height of each frame in pixels
- `frameCount` - Number of frames to extract
- `startX` - X offset to start extracting frames
- `startY` - Y offset to start extracting frames
- `columns` - Number of columns in the sprite sheet
- `rows` - Number of rows in the sprite sheet
- `frameDurationMs` - Duration to display each frame in milliseconds

**Example usage:**
```java
SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(R.drawable.spritesheet_idle)
    .frameWidth(128)
    .frameHeight(128)
    .frameCount(35)
    .startX(0)
    .startY(129)  // Second row
    .columns(35)
    .rows(1)
    .frameDurationMs(250L)
    .build();
```

### 2. SpriteSheetLoader
A utility class that loads individual frames from a sprite sheet using metadata.

**Main method:**
```java
public static Bitmap[] loadFrames(Context context, SpriteSheetMetadata metadata)
```

This method:
- Decodes the sprite sheet from resources
- Extracts individual frames based on metadata
- Handles bounds checking and error cases
- Returns an array of Bitmap frames

### 3. RunningPlayerSprite (Refactored)
The existing sprite loader has been refactored to use the metadata system instead of hardcoded values.

**Before:**
```java
private static final int FRAME_COUNT = 35;
private static final int FRAME_WIDTH = 128;
private static final int FRAME_HEIGHT = 128;
private static final int FRAME_START_Y = 129;
// ... manual frame extraction logic
```

**After:**
```java
SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(spriteSheetResId)
    .frameWidth(128)
    .frameHeight(128)
    .frameCount(35)
    .startX(0)
    .startY(129)
    .columns(35)
    .rows(1)
    .frameDurationMs(FRAME_DURATION_MS)
    .build();
    
cachedFrames = SpriteSheetLoader.loadFrames(context, spriteMetadata);
```

## Available Sprite Sheets

### spritesheet_idle.png
- Dimensions: 4480 x 512 pixels
- Frame size: 128 x 128 pixels
- Layout: 35 columns x 4 rows
- Currently used: Second row (35 frames)
- Purpose: Idle/standing animations

### spritesheet_run.png
- Dimensions: 2816 x 2048 pixels
- Frame size: 128 x 128 pixels
- Layout: 22 columns x 16 rows
- Currently unused
- Purpose: Running animations

## Usage in Field.java

The Field class uses sprite animations through RunningPlayerSprite:

```java
if (showRunningPlayerSprite) {
    runningPlayerFrames = RunningPlayerSprite.getFrames(current);
    runningPlayerLastFrameTime = SystemClock.uptimeMillis();
}
```

The animation loop in the draw method:
```java
if (showRunningPlayerSprite && runningPlayerFrames.length > 0) {
    long elapsed = now - runningPlayerLastFrameTime;
    if (elapsed >= RunningPlayerSprite.FRAME_DURATION_MS) {
        runningPlayerFrameIndex = (int) ((runningPlayerFrameIndex + framesToAdvance) % runningPlayerFrames.length);
        // ... render the current frame
    }
}
```

## Benefits

1. **Easy Configuration** - Change sprite properties without modifying extraction logic
2. **Reusability** - Use the same loader for different sprite sheets
3. **Maintainability** - Clear separation between metadata and loading logic
4. **Extensibility** - Simple to add new sprite sheets (e.g., running, jumping)
5. **Type Safety** - Builder pattern ensures valid metadata configurations

## Testing

Comprehensive unit tests verify:
- Metadata builder validates input parameters
- Loader handles null/invalid inputs gracefully
- Frame extraction works with real sprite sheets
- All test cases pass successfully

## Future Enhancements

To add the running sprite sheet:
```java
SpriteSheetMetadata runMetadata = new SpriteSheetMetadata.Builder(R.drawable.spritesheet_run)
    .frameWidth(128)
    .frameHeight(128)
    .frameCount(22)
    .startX(0)
    .startY(0)
    .columns(22)
    .rows(1)
    .frameDurationMs(150L)  // Faster animation
    .build();
```
