package com.github.lgdd.liferay.dev24.live.coding.internal.config;

import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.CONFIG_PID;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

@OCD(
    id = CONFIG_PID,
    localization = "content/Language",
    name = "our-files.config.name",
    description = "our-files.config.desc"
)
public interface OurFilesConfiguration {

  @AD(
      required = false,
      deflt = "-1",
      name = "our-files.config.form-id.name",
      description = "our-files.config.form-id.desc"
  )
  long formId();

  @AD(
      required = false,
      deflt = "1",
      name = "our-files.config.expiration-value.name",
      description = "our-files.config.expiration-value.desc"
  )
  int expirationValue();

  @AD(
      required = false,
      deflt = "week",
      name = "our-files.config.expiration-unit.name",
      description = "our-files.config.expiration-unit.desc",
      optionLabels = {
          "our-files.config.expiration-unit.second", "our-files.config.expiration-unit.minute",
          "our-files.config.expiration-unit.hour", "our-files.config.expiration-unit.day",
          "our-files.config.expiration-unit.week", "our-files.config.expiration-unit.month",
          "our-files.config.expiration-unit.year"
      },
      optionValues = {
          "second", "minute", "hour", "day", "week", "month", "year"
      }
  )
  String expirationUnit();

  @AD(
      required = false,
      deflt = "1",
      name = "our-files.config.interval.name",
      description = "our-files.config.interval.desc"
  )
  int interval();

  @AD(
      required = false,
      deflt = "hour",
      name = "our-files.config.frequency.name",
      description = "our-files.config.frequency.desc",
      optionLabels = {
          "our-files.config.frequency.second", "our-files.config.frequency.minute",
          "our-files.config.frequency.hour", "our-files.config.frequency.day",
          "our-files.config.frequency.week", "our-files.config.frequency.month"
      },
      optionValues = {
          "second", "minute", "hour", "day", "week", "month"
      }
  )
  String frequency();

}
