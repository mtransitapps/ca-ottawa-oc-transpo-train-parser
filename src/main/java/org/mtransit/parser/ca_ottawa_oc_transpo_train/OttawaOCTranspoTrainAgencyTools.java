package org.mtransit.parser.ca_ottawa_oc_transpo_train;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
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

import static org.mtransit.parser.StringUtils.EMPTY;

// https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/
// https://www.octranspo.com/fr/planifiez/outils-dinformation/developpeurs/
// https://www.octranspo.com/files/google_transit.zip
public class OttawaOCTranspoTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-ottawa-oc-transpo-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new OttawaOCTranspoTrainAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating OC Transpo train data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating OC Transpo train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		if (matcher.find()) {
			return Long.parseLong(matcher.group());
		}
		throw new MTLog.Fatal("Unexpected route ID %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			long routeId = getRouteId(gRoute);
			if (routeId == 1L) {
				return "1";
			}
			if (routeId == 2L) {
				return "2";
			}
			throw new MTLog.Fatal("Unexpected route short name %s!", gRoute);
		}
		return super.getRouteShortName(gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			long routeId = getRouteId(gRoute);
			if (routeId == 1L) {
				return "Confederation Line";
			}
			if (routeId == 2L) {
				return "Trillium Line";
			}
			if (routeId == 701L) { // R1
				return "Replacement bus service";
			}
			throw new MTLog.Fatal("Unexpected route long name %s!", gRoute);
		}
		return super.getRouteLongName(gRoute);
	}

	private static final String AGENCY_COLOR = "A2211F";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			//noinspection deprecation
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
			throw new MTLog.Fatal("Unexpected route color %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
		return tripHeadsign; // DO NOT CLEAN, USED TO IDENTIFY TRIP IN REAL TIME API // <= TODO REALLY ???
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Can't merge headsign for trips %s and %s!", mTrip, mTripToMerge);
	}

	private static final Pattern ENDS_WITH_DIRECTION = Pattern.compile("(" //
			+ "n\\.|s\\.|e\\.|w\\.|o\\." //
			+ "|" //
			+ "north/nord|south/sud|east/est|west/ouest" //
			+ "|" //
			+ "north / nord|south / sud|east / est|west / ouest" //
			+ ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern O_TRAIN_ = CleanUtils.cleanWords("o-train");
	private static final String O_TRAIN_REPLACEMENT = CleanUtils.cleanWordsReplacement(EMPTY);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = O_TRAIN_.matcher(gStopName).replaceAll(O_TRAIN_REPLACEMENT);
		gStopName = ENDS_WITH_DIRECTION.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName);
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
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
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0 && Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopId1.startsWith(EE)) {
				stopId = 100_000;
			} else if (stopId1.startsWith(EO)) {
				stopId = 200_000;
			} else if (stopId1.startsWith(NG)) {
				stopId = 300_000;
			} else if (stopId1.startsWith(NO)) {
				stopId = 400_000;
			} else if (stopId1.startsWith(WA)) {
				stopId = 500_000;
			} else if (stopId1.startsWith(WD)) {
				stopId = 600_000;
			} else if (stopId1.startsWith(WH)) {
				stopId = 700_000;
			} else if (stopId1.startsWith(WI)) {
				stopId = 800_000;
			} else if (stopId1.startsWith(WL)) {
				stopId = 900_000;
			} else if (stopId1.startsWith(PLACE)) {
				stopId = 1_000_000;
			} else if (stopId1.startsWith(RZ)) {
				stopId = 1_100_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! %s!", gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
