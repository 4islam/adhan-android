export interface AstroPosition {
    altitude: number; // in degrees
    azimuth: number;  // in degrees
}

/**
 * Simplified astronomical calculations for Sun/Moon position.
 * Base logic for a beautiful horizon visualizer.
 */
export class Astrology {
    /**
     * Accurate Sun altitude calculation.
     * Based on NOAA's Solar Calculation equations.
     */
    static getSunPosition(date: Date, lat: number, lng: number): AstroPosition {
        const rad = Math.PI / 180;
        const deg = 180 / Math.PI;

        // 1. Julian Date
        const julianDate = (date.getTime() / 86400000) + 2440587.5;
        const d = julianDate - 2451545.0;

        // 2. Solar coordinates
        const L = (280.460 + 0.9856474 * d) % 360;
        const g = (357.528 + 0.9856003 * d) % 360;
        const lambda = (L + 1.915 * Math.sin(g * rad) + 0.020 * Math.sin(2 * g * rad)) % 360;
        const epsilon = (23.439 - 0.0000004 * d) % 360;

        // 3. Right Ascension / Declination
        const alpha = Math.atan2(Math.cos(epsilon * rad) * Math.sin(lambda * rad), Math.cos(lambda * rad)) * deg;
        const delta = Math.asin(Math.sin(epsilon * rad) * Math.sin(lambda * rad)) * deg;

        // 4. Local Sidereal Time
        // GMST in hours = 18.697374558 + 24.06570982441908 * d
        const gmst = (18.697374558 + 24.06570982441908 * d) % 24;
        const lst = (gmst + lng / 15 + 24) % 24;

        // 5. Hour Angle
        let ha = (lst * 15 - alpha); // in degrees
        while (ha < -180) ha += 360;
        while (ha > 180) ha -= 360;

        // 6. Altitude
        const phi = lat * rad;
        const deltaRad = delta * rad;
        const haRad = ha * rad;

        const sinAlt = Math.sin(phi) * Math.sin(deltaRad) + Math.cos(phi) * Math.cos(deltaRad) * Math.cos(haRad);
        const altitude = Math.asin(sinAlt) * deg;

        // 7. Azimuth
        const denom = Math.cos(phi) * Math.cos(Math.asin(Math.max(-1, Math.min(1, sinAlt))));
        let azimuth = 0;
        if (Math.abs(denom) > 0.0001) {
            const cosAz = (Math.sin(deltaRad) - Math.sin(phi) * sinAlt) / denom;
            azimuth = Math.acos(Math.max(-1, Math.min(1, cosAz))) * deg;
        }
        if (Math.sin(haRad) > 0) azimuth = 360 - azimuth;

        return { altitude, azimuth };
    }

    static getMoonPosition(date: Date, lat: number, lng: number): AstroPosition {
        const rad = Math.PI / 180;
        const deg = 180 / Math.PI;

        const julianDate = (date.getTime() / 86400000) + 2440587.5;
        const d = julianDate - 2451545.0;

        // Simplified orbital elements for the Moon
        const L = (218.316 + 13.176396 * d) % 360; // Mean longitude
        const M = (134.963 + 13.064993 * d) % 360; // Mean anomaly
        const F = (93.272 + 13.229350 * d) % 360;  // Mean distance from node

        const lambda = (L + 6.289 * Math.sin(M * rad)) % 360; // Ecliptic longitude
        const beta = 5.128 * Math.sin(F * rad);               // Ecliptic latitude
        const epsilon = 23.439 * rad;                         // Obliquity

        // Right Ascension / Declination
        const alpha = Math.atan2(Math.sin(lambda * rad) * Math.cos(epsilon) - Math.tan(beta * rad) * Math.sin(epsilon), Math.cos(lambda * rad)) * deg;
        const delta = Math.asin(Math.sin(beta * rad) * Math.cos(epsilon) + Math.cos(beta * rad) * Math.sin(epsilon) * Math.sin(lambda * rad)) * deg;

        // Local Sidereal Time
        const gmst = (18.697374558 + 24.06570982441908 * d) % 24;
        const lst = (gmst + lng / 15 + 24) % 24;

        // Hour Angle
        let ha = (lst * 15 - alpha);
        while (ha < -180) ha += 360;
        while (ha > 180) ha -= 360;

        // Altitude
        const phi = lat * rad;
        const deltaRad = delta * rad;
        const haRad = ha * rad;

        const sinAlt = Math.sin(phi) * Math.sin(deltaRad) + Math.cos(phi) * Math.cos(deltaRad) * Math.cos(haRad);
        const altitude = Math.asin(Math.max(-1, Math.min(1, sinAlt))) * deg;

        // Azimuth
        const denom = Math.cos(phi) * Math.cos(Math.asin(Math.max(-1, Math.min(1, sinAlt))));
        let azimuth = 0;
        if (Math.abs(denom) > 0.0001) {
            const cosAz = (Math.sin(deltaRad) - Math.sin(phi) * sinAlt) / denom;
            azimuth = Math.acos(Math.max(-1, Math.min(1, cosAz))) * deg;
        }
        if (Math.sin(haRad) > 0) azimuth = 360 - azimuth;

        return { altitude, azimuth };
    }

    /**
     * Calculates Moonset time for a given day.
     * Uses sampling to find when altitude crosses 0.
     */
    static getMoonset(date: Date, lat: number, lng: number): Date | null {
        const startOfDay = new Date(date);
        startOfDay.setHours(0, 0, 0, 0);

        // Sample every hour to find transition
        for (let i = 0; i < 24; i++) {
            const d1 = new Date(startOfDay.getTime() + i * 3600000);
            const d2 = new Date(startOfDay.getTime() + (i + 1) * 3600000);

            const pos1 = this.getMoonPosition(d1, lat, lng);
            const pos2 = this.getMoonPosition(d2, lat, lng);

            // Moonset: going from positive altitude to negative
            if (pos1.altitude > 0 && pos2.altitude <= 0) {
                const fraction = pos1.altitude / (pos1.altitude - pos2.altitude);
                return new Date(d1.getTime() + fraction * 3600000);
            }
        }
        return null;
    }

    /**
     * Calculates Moonrise time for a given day.
     */
    static getMoonrise(date: Date, lat: number, lng: number): Date | null {
        const startOfDay = new Date(date);
        startOfDay.setHours(0, 0, 0, 0);

        for (let i = 0; i < 24; i++) {
            const d1 = new Date(startOfDay.getTime() + i * 3600000);
            const d2 = new Date(startOfDay.getTime() + (i + 1) * 3600000);

            const pos1 = this.getMoonPosition(d1, lat, lng);
            const pos2 = this.getMoonPosition(d2, lat, lng);

            // Moonrise: going from negative altitude to positive
            if (pos1.altitude <= 0 && pos2.altitude > 0) {
                const fraction = -pos1.altitude / (pos2.altitude - pos1.altitude);
                return new Date(d1.getTime() + fraction * 3600000);
            }
        }
        return null;
    }
}
