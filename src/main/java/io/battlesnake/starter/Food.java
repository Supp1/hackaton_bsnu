package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

public class Food extends Coords{
    int distance;

    public Food(int x, int y) {
        super(x, y);
    }

    public Food(JsonNode node) {
        super(node);
    }
}
