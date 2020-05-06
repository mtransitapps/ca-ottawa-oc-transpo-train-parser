package org.mtransit.parser.ca_ottawa_oc_transpo_train;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		MTLog.log("Generating OC Transpo train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating OC Transpo train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		MTLog.logFatal("Unexpected route ID %s!", gRoute);
		return -1L;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			long routeId = getRouteId(gRoute);
			if (routeId == 1L) {
				return "1";
			}
			if (routeId == 2L) {
				return "2";
			}
			MTLog.logFatal("Unexpected route short name %s!", gRoute);
			return null;
		}
		return super.getRouteShortName(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			long routeId = getRouteId(gRoute);
			if (routeId == 1L) {
				return "Confederation Line";
			}
			if (routeId == 2L) {
				return "Trillium Line";
			}
			if (routeId ==701L) { // R1
				return "Replacement bus service";
			}
			MTLog.logFatal("Unexpected route long name %s!", gRoute);
			return null;
		}
		return super.getRouteLongName(gRoute);
	}

	private static final String AGENCY_COLOR = "A2211F";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
			if (matcher.find()) {
				int routeId = Integer.parseInt(matcher.group());
				if (routeId == 1L) {
					return "DA291C";
				}
				if (routeId == 2L) {
					return "65A233";
				}
			}
			MTLog.logFatal("Unexpected route color %s!", gRoute);
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
			MTLog.logFatal("Can't merge headsign for trips %s and %s!", mTrip, mTripToMerge);
			return false; // DO NOT MERGE, USED TO IDENTIFY TRIP IN REAL TIME API
		}
		return super.mergeHeadsign(mTrip, mTripToMerge);
	}

	private static final Pattern ENDS_WITH_DIRECTION = Pattern.compile("(" //
			+ "n\\.|s\\.|e\\.|w\\.|o\\." //
			+ "|" //
			+ "north/nord|south/sud|east/est|west/ouest" //
			+ "|" //
			+ "north / nord|south / sud|east / est|west / ouest" //
			+ ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern O_TRAIN_ = CleanUtils.cleanWords("o-train");
	private static final String O_TRAIN_REPLACEMENT = CleanUtils.cleanWordsReplacement(StringUtils.EMPTY);

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
		if (stopCode.length() > 0 && Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
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
				MTLog.logFatal("Stop doesn't have an ID (start with)! %s!", gStop);
				stopId = -1;
			}
			return stopId + digits;
		}
		MTLog.logFatal("Unexpected stop ID for %s!", gStop);
		return -1;
	}
}
