package org.mtransit.parser.ca_ottawa_oc_transpo_train;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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

// https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/
// https://www.octranspo.com/fr/planifiez/outils-dinformation/developpeurs/
// https://www.octranspo.com/files/google_transit.zip
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
		System.out.printf("\nGenerating OC Transpo train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating OC Transpo train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		if (matcher.find()) {
			return Long.parseLong(matcher.group());
		}
		System.out.printf("\nUnexpected route ID %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String ROUTE_2_SHORT_NAME = "2";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (routeId == 2L) {
			return ROUTE_2_SHORT_NAME;
		}
		System.out.printf("\nUnexpected route short name %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String RLN_SEPARATOR = "-";
	private static final String RLN_SEP = " " + RLN_SEPARATOR + " ";

	private static final String O_TRAIN = "O-Train";
	private static final String GREENBORO = "Greenboro";
	private static final String BAYVIEW = "Bayview";

	private static final String ROUTE_2_LONG_NAME = O_TRAIN + " (" + BAYVIEW + RLN_SEP + GREENBORO + ")";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		long routeId = getRouteId(gRoute);
		if (routeId == 2L) {
			return ROUTE_2_LONG_NAME;
		}
		System.out.printf("\nUnexpected route long name %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR = "A2211F";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String ROUTE_COLOR_BLACK = "231F20";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
			if (matcher.find()) {
				int routeId = Integer.parseInt(matcher.group());
				// @formatter:off
				if (routeId == 2L) { return ROUTE_COLOR_BLACK; }
				// @formatter:on
			}
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return tripHeadsign; // DO NOT CLEAN, USED TO IDENTIFY TRIP IN REAL TIME API
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getHeadsignValue() == null || !mTrip.getHeadsignValue().equals(mTripToMerge.getHeadsignValue())) {
			System.out.printf("\nCan't merge headsign for trips %s and %s!\n", mTrip, mTripToMerge);
			System.exit(-1);
			return false; // DO NOT MERGE, USED TO IDENTIFY TRIP IN REAL TIME API
		}
		return super.mergeHeadsign(mTrip, mTripToMerge);
	}

	private static final Pattern ENDS_WITH_DIRECTION = Pattern.compile("(n\\.|s\\.|north/nord|south/sud)", Pattern.CASE_INSENSITIVE);

	private static final Pattern O_TRAIN_ = Pattern.compile("((^|\\W){1}(o\\-train)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String O_TRAIN_REPLACEMENT = "$2" + "$4";

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = O_TRAIN_.matcher(gStopName).replaceAll(O_TRAIN_REPLACEMENT);
		gStopName = ENDS_WITH_DIRECTION.matcher(gStopName).replaceAll(StringUtils.EMPTY);
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
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId = 0;
			if (gStop.getStopId().startsWith(EE)) {
				stopId = 100000;
			} else if (gStop.getStopId().startsWith(EO)) {
				stopId = 200000;
			} else if (gStop.getStopId().startsWith(NG)) {
				stopId = 300000;
			} else if (gStop.getStopId().startsWith(NO)) {
				stopId = 400000;
			} else if (gStop.getStopId().startsWith(WA)) {
				stopId = 500000;
			} else if (gStop.getStopId().startsWith(WD)) {
				stopId = 600000;
			} else if (gStop.getStopId().startsWith(WH)) {
				stopId = 700000;
			} else if (gStop.getStopId().startsWith(WI)) {
				stopId = 800000;
			} else if (gStop.getStopId().startsWith(WL)) {
				stopId = 900000;
			} else if (gStop.getStopId().startsWith(PLACE)) {
				stopId = 1000000;
			} else if (gStop.getStopId().startsWith(RZ)) {
				stopId = 1100000;
			} else {
				System.out.printf("\nStop doesn't have an ID (start with)! %s!\n", gStop);
				System.exit(-1);
				stopId = -1;
			}
			return stopId + digits;
		}
		System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
