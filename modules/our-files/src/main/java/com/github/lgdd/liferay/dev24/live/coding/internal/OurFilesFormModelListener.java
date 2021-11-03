package com.github.lgdd.liferay.dev24.live.coding.internal;

import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.CONFIG_PID;

import com.github.lgdd.liferay.dev24.live.coding.internal.config.OurFilesConfiguration;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecordVersion;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.util.Validator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    configurationPid = CONFIG_PID,
    service = ModelListener.class
)
public class OurFilesFormModelListener
    extends BaseModelListener<DDMFormInstanceRecordVersion> {

  @Override
  public void onAfterUpdate(DDMFormInstanceRecordVersion originalRecord,
      DDMFormInstanceRecordVersion record)
      throws ModelListenerException {

    if (_log.isDebugEnabled()) {
      _log.debug("Config FormId={}", _config.formId());
      _log.debug("Record FormId={}", record.getFormInstanceId());
    }

    if (_config.formId() == record.getFormInstanceId()) {

      if (_log.isDebugEnabled()) {
        _log.debug("formInstanceRecordId={}", record.getFormInstanceRecordId());
      }

      try {
        Map<String, String> fields = getFieldsAsMap(record);

        String emailFrom = fields.get("emailFrom");
        String[] emailTos = fields.get("emailTo").split(StringPool.COMMA);

        InternetAddress from = new InternetAddress(emailFrom);
        InternetAddress[] tos = Arrays.stream(emailTos).map(emailTo -> {
          try {
            return new InternetAddress(emailTo);
          } catch (AddressException e) {
            _log.error(e.getLocalizedMessage(), e);
          }
          return null;
        }).filter(Validator::isNotNull).toArray(InternetAddress[]::new);

        MailMessage mailMessage = new MailMessage();
        mailMessage.setFrom(from);
        mailMessage.setTo(tos);
        mailMessage.setSubject(fields.get("title"));
        mailMessage.setBody(fields.get("message"));

        _mailService.sendEmail(mailMessage);

      } catch (PortalException | AddressException e) {
        _log.error(e.getLocalizedMessage(), e);
      }
    }

    super.onAfterUpdate(originalRecord, record);
  }

  private Map<String, String> getFieldsAsMap(DDMFormInstanceRecordVersion record)
      throws PortalException {

    Locale defaultLocale = record.getDDMForm().getDefaultLocale();
    Map<String, String> fieldsAsMap = new HashMap<>();

    List<DDMFormFieldValue> fieldValues = record.getDDMFormValues().getDDMFormFieldValues();
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

  @Activate
  @Modified
  public void activate(Map<String, Object> properties) {

    _config = ConfigurableUtil.createConfigurable(OurFilesConfiguration.class, properties);
  }

  @Reference
  private MailService _mailService;

  private volatile OurFilesConfiguration _config;

  public static final Logger _log = LoggerFactory.getLogger(OurFilesFormModelListener.class);

}
