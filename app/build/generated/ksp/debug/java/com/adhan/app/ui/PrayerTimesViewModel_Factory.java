package com.adhan.app.ui;

import com.adhan.app.infra.PrayerAlarmManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class PrayerTimesViewModel_Factory implements Factory<PrayerTimesViewModel> {
  private final Provider<PrayerAlarmManager> alarmManagerProvider;

  public PrayerTimesViewModel_Factory(Provider<PrayerAlarmManager> alarmManagerProvider) {
    this.alarmManagerProvider = alarmManagerProvider;
  }

  @Override
  public PrayerTimesViewModel get() {
    return newInstance(alarmManagerProvider.get());
  }

  public static PrayerTimesViewModel_Factory create(
      Provider<PrayerAlarmManager> alarmManagerProvider) {
    return new PrayerTimesViewModel_Factory(alarmManagerProvider);
  }

  public static PrayerTimesViewModel newInstance(PrayerAlarmManager alarmManager) {
    return new PrayerTimesViewModel(alarmManager);
  }
}
