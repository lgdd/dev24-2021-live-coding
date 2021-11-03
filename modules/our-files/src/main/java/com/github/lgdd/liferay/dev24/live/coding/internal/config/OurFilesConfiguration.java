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

}
