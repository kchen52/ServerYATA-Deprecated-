import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Bus {
	private int vehicleNumber;
	private int tripID;
	private int routeNumber;
	private Direction direction;
	private String destination;
	private String pattern;
	private double longitude;
	private double latitude;
	private String recordedTime;

	public Bus() {
		vehicleNumber = -1;
		tripID = -1;
		routeNumber - -1;
		direction = Direction.NORTH;
		destination = "hell";
		pattern = "null";
		longitude = 0.0;
		latitude = 0.0;
		recordedTime = "0000";
	}

	private enum Direction {
		NORTH,SOUTH,EAST,WEST;
	}

	public void initBus(String initInput) {
		// Takes translink's input, parse it, and set values accordingly
	}

}
