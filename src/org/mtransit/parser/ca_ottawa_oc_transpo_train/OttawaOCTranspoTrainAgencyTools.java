package org.mtransit.parser.ca_ottawa_oc_transpo_train;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

// http://www.octranspo1.com/developers
// http://data.ottawa.ca/en/dataset/oc-transpo-schedules
// http://www.octranspo1.com/files/google_transit.zip
public class OttawaOCTranspoTrainAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-ottawa-oc-transpo-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new OttawaOCTranspoTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating OC Transpo train data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating OC Transpo train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.route_id);
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	private static final String ROUTE_750_SHORT_NAME = "O-Train";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (routeId == 750l) {
			return ROUTE_750_SHORT_NAME;
		} else {
			System.out.println("RSN > Unexpected route ID '" + routeId + "' (" + gRoute + ")");
			System.exit(-1);
			return null;
		}
	}

	private static final String ROUTE_750_LONG_NAME = "Bayview <-> Greenboro";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (routeId == 750l) {
			return ROUTE_750_LONG_NAME;
		} else {
			System.out.println("RLN > Unexpected route ID '" + routeId + "' (" + gRoute + ")");
			System.exit(-1);
			return null;
		}
	}

	private static final String AGENCY_COLOR = "A2211F";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String ROUTE_COLOR_BLACK = "231F20";

	@Override
	public String getRouteColor(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.route_id);
		matcher.find();
		int routeId = Integer.parseInt(matcher.group());
		// @formatter:off
		if (routeId == 750) { return ROUTE_COLOR_BLACK; }
		// @formatter:on
		else {
			System.out.println("No color for route " + gRoute + "!");
			System.exit(-1);
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.trip_headsign), gTrip.direction_id);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return tripHeadsign; // DO NOT CLEAN, USED TO IDENTIFY TRIP IN REAL TIME API
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getHeadsignValue() == null || mTrip.getHeadsignValue().equals(mTripToMerge.getHeadsignValue())) {
			System.out.println("Can't merge headsign for trips " + mTrip + " and " + mTripToMerge);
			System.exit(-1);
			return false; // DO NOT MERGE, USED TO IDENTIFY TRIP IN REAL TIME API
		}
		return super.mergeHeadsign(mTrip, mTripToMerge);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		return super.cleanStopName(gStopName);
	}

	private static final String EE = "EE";
	private static final String EO = "EO";
	private static final String NG = "NG";
	private static final String NO = "NO";
	private static final String WA = "WA";
	private static final String WD = "WD";
	private static final String WH = "WH";
	private static final String WI = "WI";
	private static final String WL = "WL";
	private static final String PLACE = "place";
	private static final String RZ = "RZ";

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0 && Utils.isDigitsOnly(stopCode)) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.stop_id);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		int stopId = 0;
		if (gStop.stop_id.startsWith(EE)) {
			stopId = 100000;
		} else if (gStop.stop_id.startsWith(EO)) {
			stopId = 200000;
		} else if (gStop.stop_id.startsWith(NG)) {
			stopId = 300000;
		} else if (gStop.stop_id.startsWith(NO)) {
			stopId = 400000;
		} else if (gStop.stop_id.startsWith(WA)) {
			stopId = 500000;
		} else if (gStop.stop_id.startsWith(WD)) {
			stopId = 600000;
		} else if (gStop.stop_id.startsWith(WH)) {
			stopId = 700000;
		} else if (gStop.stop_id.startsWith(WI)) {
			stopId = 800000;
		} else if (gStop.stop_id.startsWith(WL)) {
			stopId = 900000;
		} else if (gStop.stop_id.startsWith(PLACE)) {
			stopId = 1000000;
		} else if (gStop.stop_id.startsWith(RZ)) {
			stopId = 1100000;
		} else {
			System.out.println("Stop doesn't have an ID (start with)! " + gStop);
			System.exit(-1);
			stopId = -1;
		}
		return stopId + digits;
	}
}
