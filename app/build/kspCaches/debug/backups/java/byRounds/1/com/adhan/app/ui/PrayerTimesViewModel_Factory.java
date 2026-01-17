package com.adhan.app.ui;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
  @Override
  public PrayerTimesViewModel get() {
    return newInstance();
  }

  public static PrayerTimesViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PrayerTimesViewModel newInstance() {
    return new PrayerTimesViewModel();
  }

  private static final class InstanceHolder {
    private static final PrayerTimesViewModel_Factory INSTANCE = new PrayerTimesViewModel_Factory();
  }
}
