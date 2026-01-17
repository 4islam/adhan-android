
// Append this to valid JS
var pt = new PrayTime();

var testCases = [
    {
        name: "Makkah_2024_01_01",
        date: new Date(Date.UTC(2024, 0, 1)), // Use UTC date to avoid confusion, but we pass explicit components
        year: 2024, month: 1, day: 1,
        lat: 21.4225,
        lng: 39.8262,
        timezone: 3,
        method: pt.Makkah,
        format: pt.Time24
    },
    {
        name: "NYC_2024_06_15",
        year: 2024, month: 6, day: 15,
        lat: 40.7128,
        lng: -74.0060,
        timezone: -4,
        method: pt.ISNA,
        format: pt.Time24
    },
    {
        name: "London_2024_12_21",
        year: 2024, month: 12, day: 21,
        lat: 51.5074,
        lng: -0.1278,
        timezone: 0,
        method: pt.MWL,
        format: pt.Time24
    }
];

var results = {};

for (var i = 0; i < testCases.length; i++) {
    var tc = testCases[i];
    pt.setCalcMethod(tc.method);
    pt.setTimeFormat(tc.format);

    var times = pt.getDatePrayerTimes(
        tc.year,
        tc.month,
        tc.day,
        tc.lat,
        tc.lng,
        tc.timezone
    );
    results[tc.name] = times;
}

console.log(JSON.stringify(results, null, 2));
