package com.github.lgdd.liferay.dev24.live.coding.api;

import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecordVersion;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.Validator;
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

  public static final String MULTIPLE_VALUES_DELIMITER = StringPool.COMMA;

  /**
   * Transform form names and values from a form record to a map. Field references are used as keys.
   * If a field is repeatable, the values are stored in the same key, separated by the given
   * delimiter.
   *
   * @param record                  instance record of a Liferay Form
   * @param multipleValuesDelimiter delimiter to use to separate values of the same key. If null,
   *                                {@link #MULTIPLE_VALUES_DELIMITER} is used as default.
   * @return a map with fields name as key and fields value(s) as value PortalException if the form
   * values can't be parsed properly
   */
  public Map<String, String> getFieldsAsMap(DDMFormInstanceRecordVersion record,
      String multipleValuesDelimiter)
      throws PortalException {

    final String delimiter = Validator.isNotNull(multipleValuesDelimiter) ?
                             multipleValuesDelimiter : MULTIPLE_VALUES_DELIMITER;
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
