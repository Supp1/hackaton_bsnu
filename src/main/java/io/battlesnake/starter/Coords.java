package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

public class Coords {
    int x;
    int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public Coords(JsonNode node) {
        this.x = node.get("x").asInt();
        this.y = node.get("y").asInt();
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Coords)) return false;
        final Coords other = (Coords) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getX() != other.getX()) return false;
        if (this.getY() != other.getY()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Coords;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getX();
        result = result * PRIME + this.getY();
        return result;
    }
}
