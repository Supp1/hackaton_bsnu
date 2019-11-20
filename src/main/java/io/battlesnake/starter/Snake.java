package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    public static int BOARD_SIZE;

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            port = "8080";
            LOG.info("Using default port: {}", port);
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Battlesnake documentation can be found at " +
                "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>.");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @param //pingRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return an empty response.
         */
        public Map<String, String> ping() {
            return EMPTY;
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            Map<String, String> response = new HashMap<>();
            BOARD_SIZE = startRequest.get("board").get("width").asInt() - 1;
            LOG.info(Integer.toString(BOARD_SIZE));
            response.put("color", "#420069");
            return response;
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        public Map<String, String> move(JsonNode moveRequest) {
            Map<String, String> response = new HashMap<>();

            List<JsonNode> snakeBody = new ArrayList<>();
            List<JsonNode> snakes = new ArrayList<>();
            List<Coords> barriers = new ArrayList<>();
            List<Coords> foods = new ArrayList<>();

            JsonNode mySnake = moveRequest.get("you");
            mySnake.get("body").elements().forEachRemaining(snakeBody::add);
            Coords snakeHead = new Coords(snakeBody.get(0));

            moveRequest.get("board").get("snakes").elements().forEachRemaining((obj) -> {
                snakes.add(obj);
            });
            moveRequest.get("board").get("food").elements().forEachRemaining((obj) -> {
                foods.add(new Coords(obj));
            });

            List<List<Coords>> bariersCoord = snakes.stream()
                    .filter((obj) -> {
                        if (obj.get("id").asText().trim() == mySnake.get("id").asText().trim()) {
                            return false;
                        }
                        return true;
                    })
                    .map((obj) -> obj.get("body"))
                    .map((obj) -> {
                        List<Coords> arr = new ArrayList<>();
                        obj.elements().forEachRemaining((coordinate) -> {
                            arr.add(new Coords(coordinate));
                        });
                        return arr;
                    })
                    .collect(Collectors.toList());
            bariersCoord.forEach((list) -> {
                list.forEach(barriers::add);
            });

            foods.sort((o1, o2) -> {
                int res1 = Math.abs(o1.getX() - snakeHead.getX()) + Math.abs(o1.getY() - snakeHead.getY());
                int res2 = Math.abs(o2.getX() - snakeHead.getX()) + Math.abs(o2.getY() - snakeHead.getY());
                return res1 - res2;
            });
            LOG.info("**********SORTED FOOODS*********\n" + foods.toString());
            if (isFree(barriers, snakeHead.getX() + 1, snakeHead.getY())) {
                //ВПРАВО
                response.put("move", "right");
            }
            if (isFree(barriers, snakeHead.getX(), snakeHead.getY() - 1)) {
                //ВВЕРХ
                response.put("move", "up");
            }
            if (isFree(barriers, snakeHead.getX() - 1, snakeHead.getY())) {
                //ВЛЕВО
                response.put("move", "left");
            }
            if (isFree(barriers, snakeHead.getX(), snakeHead.getY() + 1)) {
                //ВНИЗ
                response.put("move", "down");
            }
            return response;
        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            Map<String, String> response = new HashMap<>();
            return response;
        }

        private JsonNode getHead(JsonNode snake) {
            return snake.get("body").elements().next();
        }

        private boolean isFree(List<Coords> bariers, int x, int y) {
            Coords coord = new Coords(x, y);
            return !(bariers.contains(coord))
                    && coord.getX() >= 0 && coord.getX() <= BOARD_SIZE
                    && coord.getY() >= 0 && coord.getY() <= BOARD_SIZE;
        }
    }
}


//            if (snakeHead.getX() < BOARD_SIZE - 8) {
//
//            }
//            if (snakeHead.getY() < BOARD_SIZE - 8) {
//                response.put("move", "right");
//            }
//            if (snakeHead.getX() > BOARD_SIZE - 3 && snakeHead.getY() < BOARD_SIZE - 3) {
//                response.put("move", "down");
//            }
//            if (snakeHead.getY() > BOARD_SIZE - 3) {
//                response.put("move", "left");
//            }