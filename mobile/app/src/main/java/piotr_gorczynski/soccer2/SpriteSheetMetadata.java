package piotr_gorczynski.soccer2;

/**
 * Metadata describing the layout and properties of a sprite sheet.
 * This allows for easy configuration of sprite animations without hardcoding values.
 */
public class SpriteSheetMetadata {
    private final int resourceId;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int startX;
    private final int startY;
    private final int columns;
    private final int rows;
    private final long frameDurationMs;

    private SpriteSheetMetadata(Builder builder) {
        this.resourceId = builder.resourceId;
        this.frameWidth = builder.frameWidth;
        this.frameHeight = builder.frameHeight;
        this.frameCount = builder.frameCount;
        this.startX = builder.startX;
        this.startY = builder.startY;
        this.columns = builder.columns;
        this.rows = builder.rows;
        this.frameDurationMs = builder.frameDurationMs;
    }

    public int getResourceId() {
        return resourceId;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public long getFrameDurationMs() {
        return frameDurationMs;
    }

    /**
     * Builder pattern for creating SpriteSheetMetadata instances.
     */
    public static class Builder {
        private int resourceId;
        private int frameWidth;
        private int frameHeight;
        private int frameCount;
        private int startX = 0;
        private int startY = 0;
        private int columns = 1;
        private int rows = 1;
        private long frameDurationMs = 100L;

        public Builder(int resourceId) {
            this.resourceId = resourceId;
        }

        public Builder frameWidth(int frameWidth) {
            this.frameWidth = frameWidth;
            return this;
        }

        public Builder frameHeight(int frameHeight) {
            this.frameHeight = frameHeight;
            return this;
        }

        public Builder frameCount(int frameCount) {
            this.frameCount = frameCount;
            return this;
        }

        public Builder startX(int startX) {
            this.startX = startX;
            return this;
        }

        public Builder startY(int startY) {
            this.startY = startY;
            return this;
        }

        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public Builder frameDurationMs(long frameDurationMs) {
            this.frameDurationMs = frameDurationMs;
            return this;
        }

        public SpriteSheetMetadata build() {
            if (frameWidth <= 0 || frameHeight <= 0) {
                throw new IllegalArgumentException("Frame dimensions must be positive");
            }
            if (frameCount <= 0) {
                throw new IllegalArgumentException("Frame count must be positive");
            }
            if (frameDurationMs < 0) {
                throw new IllegalArgumentException("Frame duration must not be negative");
            }
            return new SpriteSheetMetadata(this);
        }
    }
}
