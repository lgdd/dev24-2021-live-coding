package com.github.lgdd.liferay.dev24.live.coding.api;

import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecordVersion;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = OurFilesFormService.class
)
public class OurFilesFormService {

  public Map<String, String> getFieldsAsMap(DDMFormInstanceRecordVersion record)
      throws PortalException {

    final Locale defaultLocale = record.getDDMForm().getDefaultLocale();
    final Map<String, String> fieldsAsMap = new HashMap<>();

    final List<DDMFormFieldValue> fieldValues = record.getDDMFormValues().getDDMFormFieldValues();
    fieldValues.forEach(fieldValue -> {

      String key = fieldValue.getFieldReference();
      String value = fieldValue.getValue().getString(defaultLocale);

      fieldsAsMap.merge(key, value, (previousValue, currentValue) ->
          previousValue.concat(StringPool.COMMA + currentValue)
      );

    });

    if (_log.isDebugEnabled()) {
      for (Entry<String, String> entry : fieldsAsMap.entrySet()) {
        _log.debug("[{}]={}", entry.getKey(), entry.getValue());
      }
    }

    return fieldsAsMap;
  }

  public static final Logger _log = LoggerFactory.getLogger(OurFilesFormService.class);

}
