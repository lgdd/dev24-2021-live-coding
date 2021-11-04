package com.github.lgdd.liferay.dev24.live.coding.internal;

import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.CONFIG_PID;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_EMAIL_FROM;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_EMAIL_TO;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_FILE_TO_SHARE;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_MESSAGE;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_TITLE;

import com.github.lgdd.liferay.dev24.live.coding.api.OurFilesFormService;
import com.github.lgdd.liferay.dev24.live.coding.internal.config.OurFilesConfiguration;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecordVersion;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.util.Validator;
import java.util.Arrays;
import java.util.Map;
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

      _sendEmail(record);

    }

    super.onAfterUpdate(originalRecord, record);
  }

  /**
   * Email each recipient using the given sender email, title and message to build the mail
   * message.
   *
   * @param record instance record of a Liferay Form
   */
  private void _sendEmail(DDMFormInstanceRecordVersion record) {

    try {
      final Map<String, String> fields = _ourFilesFormService.getFieldsAsMap(record,
                                                                             StringPool.COMMA);

      final String emailFrom = fields.get(FIELD_EMAIL_FROM);
      final String[] emailTos = fields.get(FIELD_EMAIL_TO).split(StringPool.COMMA);

      final InternetAddress from = new InternetAddress(emailFrom);
      final InternetAddress[] tos = Arrays.stream(emailTos).map(emailTo -> {
        try {
          return new InternetAddress(emailTo);
        } catch (AddressException e) {
          _log.error(e.getLocalizedMessage(), e);
        }
        return null;
      }).filter(Validator::isNotNull).toArray(InternetAddress[]::new);

      final JSONObject fileToShare = JSONFactoryUtil.createJSONObject(
          fields.get(FIELD_FILE_TO_SHARE));

      final String mailBody = _buildHTMLBody(fields.get(FIELD_MESSAGE), fileToShare);

      final MailMessage mailMessage = new MailMessage();
      mailMessage.setFrom(from);
      mailMessage.setTo(tos);
      mailMessage.setSubject(fields.get(FIELD_TITLE));
      mailMessage.setBody(mailBody);
      mailMessage.setHTMLFormat(true);

      _mailService.sendEmail(mailMessage);

    } catch (PortalException | AddressException e) {
      _log.error(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Build the HTML mail body message.
   *
   * @param message     message from the sender
   * @param fileToShare file to share as JSON object
   * @return string as HTML mail body containing the message and a link to download the file to
   * share
   */
  private String _buildHTMLBody(String message, JSONObject fileToShare) {

    final StringBundler mailBody = new StringBundler();
    mailBody.append("<html>");
    mailBody.append("<head>");
    mailBody.append("<title>Our Files</title>");
    mailBody.append("<meta charset=\"UTF-8\">");
    mailBody.append("<body>");
    mailBody.append("<p>");
    mailBody.append(message);
    mailBody.append("</p>");
    mailBody.append("<p>");
    mailBody.append("<a href=\"");
    mailBody.append(fileToShare.get("url"));
    mailBody.append("\">");
    mailBody.append(fileToShare.get("title"));
    mailBody.append("</a>");
    mailBody.append("</p>");
    mailBody.append("</body>");
    mailBody.append("</head>");
    mailBody.append("</html>");

    return mailBody.toString();
  }

  @Activate
  @Modified
  public void activate(Map<String, Object> properties) {

    _config = ConfigurableUtil.createConfigurable(OurFilesConfiguration.class, properties);
  }

  @Reference
  private MailService _mailService;

  @Reference
  private OurFilesFormService _ourFilesFormService;

  private volatile OurFilesConfiguration _config;

  public static final Logger _log = LoggerFactory.getLogger(OurFilesFormModelListener.class);

}
