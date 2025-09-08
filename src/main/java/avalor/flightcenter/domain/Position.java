package avalor.flightcenter.domain;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public class Position {
    private final int MAX_DECAY_VAL = Integer.MAX_VALUE;
    private final int posX;
    private final int posY;
    private final int value;
    private int decay;
    private boolean isOccupied;

    public Position(int posX, int posY, int value) {
        this.posX = posX;
        this.posY = posY;
        this.value = value;
        this.decay = 0;
        this.isOccupied = false;
    }

    // --- Builder ---
    public static Builder builder() { return new Builder(); }
    public static Builder builder(Position src) {
        if (src == null) { return new Builder(); }
        return new Builder()
                .posX(src.posX)
                .posY(src.posY)
                .value(src.value)
                .decay(src.decay)
                .occupied(src.isOccupied);
    }

    public static final class Builder {
        private int posX;
        private int posY;
        private int value;
        private int decay;
        private boolean isOccupied;

        public Builder posX(int v) { this.posX = v; return this; }
        public Builder posY(int v) { this.posY = v; return this; }
        public Builder value(int v) { this.value = v; return this; }
        public Builder decay(int v) { this.decay = v; return this; }
        public Builder occupied(boolean v) { this.isOccupied = v; return this; }

        public Position build() {
            Position p = new Position(posX, posY, value);
            p.setDecay(decay);
            p.setOccupied(isOccupied);
            return p;
        }
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getValue() {
        return value;
    }

    public int getDecay() {
        return decay;
    }

    public void setDecay(int decay) {
        if (decay <= MAX_DECAY_VAL) {
            this.decay = decay;
        } else {
            this.decay = MAX_DECAY_VAL;
        }
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) {
            return false;
        }
        return this.posX == ((Position) obj).posX && this.posY == ((Position) obj).posY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posX, posY);
    }
}
