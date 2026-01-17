package com.adhan.app.domain.models

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

class PrayerTimesCalculator {

    // Calculation Methods
    companion object {
        const val Jafari = 0
        const val Karachi = 1
        const val ISNA = 2
        const val MWL = 3
        const val Makkah = 4
        const val Egypt = 5
        const val Custom = 6
        const val Tehran = 7
        const val Ahmadiyya = 8

        // Juristic Methods
        const val Shafii = 0
        const val Hanafi = 1

        // Adjusting Methods for Higher Latitudes
        const val None = 0
        const val MidNight = 1
        const val OneSeventh = 2
        const val AngleBased = 3

        // Time Formats
        const val Time24 = 0
        const val Time12 = 1
        const val Time12NS = 2
        const val Float = 3

        val timeNames = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Sunset", "Maghrib", "Isha")
        const val InvalidTime = "-----"
    }

    private var calcMethod = Ahmadiyya
    private var asrJuristic = Shafii
    private var dhuhrMinutes = 0
    private var adjustHighLats = AngleBased
    private var timeFormat = Time24
    private var smartCombining = true
    private var combiningThreshold = 90 // Minutes

    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var timeZone: Double = 0.0
    private var jDate: Double = 0.0

    private var numIterations = 1

    private val methodParams: MutableMap<Int, DoubleArray> = mutableMapOf()

    init {
        // fa : fajr angle
        // ms : maghrib selector (0 = angle; 1 = minutes after sunset)
        // mv : maghrib parameter value (in angle or minutes)
        // is : isha selector (0 = angle; 1 = minutes after maghrib)
        // iv : isha parameter value (in angle or minutes)

        methodParams[Jafari] = doubleArrayOf(16.0, 0.0, 4.0, 0.0, 14.0)
        methodParams[Karachi] = doubleArrayOf(18.0, 1.0, 0.0, 0.0, 18.0)
        methodParams[ISNA] = doubleArrayOf(15.0, 1.0, 1.0, 0.0, 15.0)
        methodParams[MWL] = doubleArrayOf(18.0, 1.0, 0.0, 0.0, 17.0)
        methodParams[Makkah] = doubleArrayOf(19.0, 1.0, 0.0, 1.0, 90.0)
        methodParams[Egypt] = doubleArrayOf(19.5, 1.0, 0.0, 0.0, 17.5)
        methodParams[Tehran] = doubleArrayOf(17.7, 0.0, 4.5, 0.0, 15.0)
        methodParams[Custom] = doubleArrayOf(18.0, 1.0, 0.0, 0.0, 17.0)
        methodParams[Ahmadiyya] = doubleArrayOf(14.5, 1.0, 1.0, 0.0, 12.3)
    }

    // ---------------------- Public Interface -----------------------

    fun getPrayerTimes(date: Date, latitude: Double, longitude: Double, tZone: Double? = null): List<String> {
        val cal = Calendar.getInstance()
        cal.time = date
        return getDatePrayerTimes(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            latitude,
            longitude,
            tZone ?: 999.0 // Sentinel if null
        )
    }

    fun getDatePrayerTimes(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, tZone: Double): List<String> {
        this.lat = latitude
        this.lng = longitude
        this.timeZone = effectiveTimeZone(year, month, day, tZone)
        this.jDate = julianDate(year, month, day) - longitude / (15 * 24)
        return computeDayTimes()
    }

    fun setCalcMethod(methodID: Int) {
        calcMethod = methodID
    }

    fun setAsrMethod(methodID: Int) {
        if (methodID < 0 || methodID > 1) return
        asrJuristic = methodID
    }

    fun setFajrAngle(angle: Double) {
        setCustomParams(doubleArrayOf(angle, -1.0, -1.0, -1.0, -1.0))
    }

    fun setMaghribAngle(angle: Double) {
        setCustomParams(doubleArrayOf(-1.0, 0.0, angle, -1.0, -1.0))
    }

    fun setIshaAngle(angle: Double) {
        setCustomParams(doubleArrayOf(-1.0, -1.0, -1.0, 0.0, angle))
    }

    fun setDhuhrMinutes(minutes: Int) {
        dhuhrMinutes = minutes
    }

    fun setMaghribMinutes(minutes: Double) {
        setCustomParams(doubleArrayOf(-1.0, 1.0, minutes, -1.0, -1.0))
    }

    fun setIshaMinutes(minutes: Double) {
        setCustomParams(doubleArrayOf(-1.0, -1.0, -1.0, 1.0, minutes))
    }

    private fun setCustomParams(params: DoubleArray) {
        for (i in 0 until 5) {
            if (params[i] == -1.0) {
                // -1.0 acts as 'null' in this context
                methodParams[Custom]!![i] = methodParams[calcMethod]!![i]
            } else {
                methodParams[Custom]!![i] = params[i]
            }
        }
        calcMethod = Custom
    }

    fun setHighLatsMethod(methodID: Int) {
        adjustHighLats = methodID
    }

    fun setTimeFormat(timeFormat: Int) {
        this.timeFormat = timeFormat
    }

    fun setSmartCombining(enabled: Boolean) {
        smartCombining = enabled
    }

    fun setCombiningThreshold(minutes: Int) {
        combiningThreshold = minutes
    }

    data class CombinedPrayerInfo(
        val name: String,
        val time: String,
        val isCombined: Boolean = false,
        val originalNames: List<String> = emptyList()
    )

    // ---------------------- Calculation Functions -----------------------

    private fun sunPosition(jd: Double): DoubleArray {
        val D = jd - 2451545.0
        val g = fixangle(357.529 + 0.98560028 * D)
        val q = fixangle(280.459 + 0.98564736 * D)
        val L = fixangle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))

        val R = 1.00014 - 0.01671 * dcos(g) - 0.00014 * dcos(2 * g)
        val e = 23.439 - 0.00000036 * D

        val d = darcsin(dsin(e) * dsin(L))
        var RA = darctan2(dcos(e) * dsin(L), dcos(L)) / 15
        RA = fixhour(RA)
        val EqT = q / 15 - RA

        return doubleArrayOf(d, EqT)
    }

    private fun equationOfTime(jd: Double): Double {
        return sunPosition(jd)[1]
    }

    private fun sunDeclination(jd: Double): Double {
        return sunPosition(jd)[0]
    }

    private fun computeMidDay(t: Double): Double {
        val T = equationOfTime(jDate + t)
        return fixhour(12 - T)
    }

    private fun computeTime(G: Double, t: Double): Double {
        val D = sunDeclination(jDate + t)
        val Z = computeMidDay(t)
        val V = 1.0 / 15.0 * darccos((-dsin(G) - dsin(D) * dsin(lat)) / (dcos(D) * dcos(lat)))
        return Z + if (G > 90) -V else V
    }

    private fun computeAsr(step: Int, t: Double): Double {
        val D = sunDeclination(jDate + t)
        val G = -darccot(step + dtan(abs(lat - D)))
        return computeTime(G, t)
    }

    private fun computeTimes(times: DoubleArray): DoubleArray {
        val t = dayPortion(times)

        val Fajr = computeTime(180 - methodParams[calcMethod]!![0], t[0])
        val Sunrise = computeTime(180 - 0.833, t[1])
        val Dhuhr = computeMidDay(t[2])
        val Asr = computeAsr(1 + asrJuristic, t[3])
        val Sunset = computeTime(0.833, t[4])
        val Maghrib = computeTime(methodParams[calcMethod]!![2], t[5])
        val Isha = computeTime(methodParams[calcMethod]!![4], t[6])

        return doubleArrayOf(Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha)
    }

    private fun computeDayTimes(): List<String> {
        var times = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0, 18.0) // default times
        for (i in 1..numIterations) {
            times = computeTimes(times)
        }
        times = adjustTimes(times)
        
        return adjustTimesFormat(times)
    }

    fun getCombinedPrayerTimes(date: Date, latitude: Double, longitude: Double, tZone: Double? = null): List<CombinedPrayerInfo> {
        val times = getPrayerTimes(date, latitude, longitude, tZone)
        val names = timeNames
        
        val result = mutableListOf<CombinedPrayerInfo>()
        var i = 0
        while (i < names.size) {
            val name = names[i]
            val time = times[i]
            
            if (smartCombining) {
                // Combine Dhuhr (2) and Asr (3)
                if (name == "Dhuhr" && i + 1 < names.size && names[i+1] == "Asr") {
                    val dhuhrMinutes = timeToMinutes(time)
                    val asrMinutes = timeToMinutes(times[i+1])
                    if (asrMinutes - dhuhrMinutes <= combiningThreshold) {
                        result.add(CombinedPrayerInfo("Dhuhr/Asr", time, true, listOf("Dhuhr", "Asr")))
                        i += 2
                        continue
                    }
                }
                // Combine Maghrib (5) and Isha (6)
                if (name == "Maghrib" && i + 1 < names.size && names[i+1] == "Isha") {
                    val maghribMinutes = timeToMinutes(time)
                    val ishaMinutes = timeToMinutes(times[i+1])
                    if (ishaMinutes - maghribMinutes <= combiningThreshold) {
                        result.add(CombinedPrayerInfo("Maghrib/Isha", time, true, listOf("Maghrib", "Isha")))
                        i += 2
                        continue
                    }
                }
            }
            
            result.add(CombinedPrayerInfo(name, time))
            i++
        }
        return result
    }

    private fun timeToMinutes(time: String): Int {
        if (time == InvalidTime) return 0
        val parts = time.split(":")
        if (parts.size < 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * More accurate Moonrise and Moonset calculation
     * Based on Meeus algorithms with basic perturbations.
     */
    fun getMoonTimes(date: Date, latitude: Double, longitude: Double, tZone: Double? = null): Map<String, String> {
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        val effectiveTZone = effectiveTimeZone(year, month, day, tZone ?: 999.0)
        
        fun moonAltitude(hour: Double): Double {
            val jd = julianDate(year, month, day) + hour / 24.0 - longitude / (15 * 24.0)
            val d = jd - 2451545.0
            
            // Moon's mean elements
            val Lprime = fixangle(218.316 + 13.176396 * d) // mean longitude
            val Mprime = fixangle(134.963 + 13.064993 * d) // mean anomaly
            val F = fixangle(93.272 + 13.229350 * d) // distance from node
            val D = fixangle(297.850 + 12.190749 * d) // mean elongation
            
            // Main perturbations in longitude
            val moonLon = Lprime + 6.289 * dsin(Mprime) + 1.274 * dsin(2 * D - Mprime) + 
                          0.658 * dsin(2 * D) + 0.214 * dsin(2 * Mprime)
            
            // Main perturbations in latitude
            val moonLat = 5.128 * dsin(F) + 0.280 * dsin(Mprime + F) + 0.277 * dsin(Mprime - F)
            
            // Obliquity of ecliptic
            val ecl = 23.439 - 0.00000036 * d
            
            // Equatorial coordinates
            val ra = darctan2(dcos(ecl) * dsin(moonLon) - dsin(ecl) * dtan(moonLat), dcos(moonLon))
            val dec = darcsin(dsin(ecl) * dsin(moonLon) * dcos(moonLat) + dcos(ecl) * dsin(moonLat))
            
            // Sidereal time
            val mDegrees = 280.46061837 + 360.98564736629 * d
            val lst = fixangle(mDegrees + longitude)
            
            val ha = fixangle(lst - ra * 15.0)
            
            val alt = darcsin(dsin(latitude) * dsin(dec) + dcos(latitude) * dcos(dec) * dcos(ha))
            return alt
        }

        // Iterative search for rise/set
        var rise: Double? = null
        var set: Double? = null
        
        val h0 = -0.833 // standard refraction/size correction
        var prevAlt = moonAltitude(0.0)
        
        for (h in 1..24) {
            val hour = h.toDouble()
            val alt = moonAltitude(hour)
            
            if (prevAlt <= h0 && alt > h0) {
                // Rising
                rise = hour - (alt - h0) / (alt - prevAlt)
            } else if (prevAlt >= h0 && alt < h0) {
                // Setting
                set = hour - (alt - h0) / (alt - prevAlt)
            }
            prevAlt = alt
        }

        return mapOf(
            "Moonrise" to if (rise != null) floatToTime24(fixhour(rise + effectiveTZone)) else InvalidTime,
            "Moonset" to if (set != null) floatToTime24(fixhour(set + effectiveTZone)) else InvalidTime
        )
    }

    private fun adjustTimes(times: DoubleArray): DoubleArray {
        for (i in 0 until 7) {
            times[i] += timeZone - lng / 15
        }
        times[2] += dhuhrMinutes / 60.0 // Dhuhr

        if (methodParams[calcMethod]!![1] == 1.0) { // Maghrib
            times[5] = times[4] + methodParams[calcMethod]!![2] / 60.0
        }
        if (methodParams[calcMethod]!![3] == 1.0) { // Isha
            times[6] = times[5] + methodParams[calcMethod]!![4] / 60.0
        }

        if (adjustHighLats != None) {
            return adjustHighLatTimes(times)
        }
        return times
    }

    private fun adjustTimesFormat(times: DoubleArray): List<String> {
        val result = mutableListOf<String>()
        if (timeFormat == Float) {
            return times.map { it.toString() }
        }
        for (i in 0 until 7) {
            if (timeFormat == Time12) {
                result.add(floatToTime12(times[i]))
            } else if (timeFormat == Time12NS) {
                result.add(floatToTime12(times[i], true))
            } else {
                result.add(floatToTime24(times[i]))
            }
        }
        return result
    }

    private fun adjustHighLatTimes(times: DoubleArray): DoubleArray {
        val nightTime = timeDiff(times[4], times[1]) // sunset to sunrise

        // Adjust Fajr
        val FajrDiff = nightPortion(methodParams[calcMethod]!![0]) * nightTime
        if (times[0].isNaN() || timeDiff(times[0], times[1]) > FajrDiff) {
            times[0] = times[1] - FajrDiff
        }

        // Adjust Isha
        val IshaAngle = if (methodParams[calcMethod]!![3] == 0.0) methodParams[calcMethod]!![4] else 18.0
        val IshaDiff = nightPortion(IshaAngle) * nightTime
        if (times[6].isNaN() || timeDiff(times[4], times[6]) > IshaDiff) {
            times[6] = times[4] + IshaDiff
        }

        // Adjust Maghrib
        val MaghribAngle = if (methodParams[calcMethod]!![1] == 0.0) methodParams[calcMethod]!![2] else 4.0
        val MaghribDiff = nightPortion(MaghribAngle) * nightTime
        if (times[5].isNaN() || timeDiff(times[4], times[5]) > MaghribDiff) {
            times[5] = times[4] + MaghribDiff
        }

        return times
    }

    private fun nightPortion(angle: Double): Double {
        return when (adjustHighLats) {
            AngleBased -> 1.0 / 60.0 * angle
            MidNight -> 1.0 / 2.0
            OneSeventh -> 1.0 / 7.0
            else -> 0.0
        }
    }

    private fun dayPortion(times: DoubleArray): DoubleArray {
        for (i in 0 until 7) {
            times[i] /= 24.0
        }
        return times
    }

    // ---------------------- Misc Functions -----------------------

    private fun timeDiff(time1: Double, time2: Double): Double {
        return fixhour(time2 - time1)
    }

    private fun twoDigitsFormat(num: Int): String {
        return if (num < 10) "0$num" else num.toString()
    }

    private fun floatToTime24(time: Double): String {
        if (time.isNaN()) return InvalidTime
        var t = fixhour(time + 0.5 / 60) // add 0.5 minutes to round
        val hours = floor(t).toInt()
        val minutes = floor((t - hours) * 60).toInt()
        return "${twoDigitsFormat(hours)}:${twoDigitsFormat(minutes)}"
    }

    private fun floatToTime12(time: Double, noSuffix: Boolean = false): String {
        if (time.isNaN()) return InvalidTime
        var t = fixhour(time + 0.5 / 60) // add 0.5 minutes to round
        var hours = floor(t).toInt()
        val minutes = floor((t - hours) * 60).toInt()
        val suffix = if (hours >= 12) " pm" else " am"
        hours = (hours + 12 - 1) % 12 + 1
        return "$hours:${twoDigitsFormat(minutes)}${if (noSuffix) "" else suffix}"
    }

    // ---------------------- Julian Date Functions -----------------------

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = floor(y / 100.0)
        val B = 2 - A + floor(A / 4.0)
        
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5
    }

    // ---------------------- Time-Zone Functions -----------------------

    private fun effectiveTimeZone(year: Int, month: Int, day: Int, tZone: Double): Double {
        if (tZone == 999.0) { // Sentinel for 'auto'
             // In Kotlin/Java getting purely local timezone offset for historical dates can apply DST
             // The JS code uses: new Date(year, month - 1, day) and compares to GMT
             val cal = Calendar.getInstance()
             cal.set(year, month - 1, day)
             val zone = TimeZone.getDefault()
             // Offset in milliseconds
             val offset = zone.getOffset(cal.timeInMillis)
             return offset / (1000.0 * 60 * 60)
        }
        return tZone
    }

    // ---------------------- Trigonometric Functions -----------------------

    private fun dsin(d: Double): Double {
        return sin(dtr(d))
    }

    private fun dcos(d: Double): Double {
        return cos(dtr(d))
    }

    private fun dtan(d: Double): Double {
        return tan(dtr(d))
    }

    private fun darcsin(x: Double): Double {
        return rtd(asin(x))
    }

    private fun darccos(x: Double): Double {
        return rtd(acos(x))
    }

    private fun darctan(x: Double): Double {
        return rtd(atan(x))
    }

    private fun darctan2(y: Double, x: Double): Double {
        return rtd(atan2(y, x))
    }

    private fun darccot(x: Double): Double {
        return rtd(atan(1.0 / x))
    }

    private fun dtr(d: Double): Double {
        return (d * PI) / 180.0
    }

    private fun rtd(r: Double): Double {
        return (r * 180.0) / PI
    }

    private fun fixangle(a: Double): Double {
        var angle = a - 360.0 * floor(a / 360.0)
        angle = if (angle < 0) angle + 360.0 else angle
        return angle
    }

    private fun fixhour(a: Double): Double {
        var hour = a - 24.0 * floor(a / 24.0)
        hour = if (hour < 0) hour + 24.0 else hour
        return hour
    }
}
