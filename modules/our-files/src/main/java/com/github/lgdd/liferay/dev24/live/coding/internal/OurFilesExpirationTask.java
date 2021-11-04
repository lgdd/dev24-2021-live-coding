package com.github.lgdd.liferay.dev24.live.coding.internal;


import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.CONFIG_PID;
import static com.github.lgdd.liferay.dev24.live.coding.api.OurFilesConstants.FIELD_FILE_TO_SHARE;

import com.github.lgdd.liferay.dev24.live.coding.api.OurFilesFormService;
import com.github.lgdd.liferay.dev24.live.coding.internal.config.OurFilesConfiguration;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryService;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordLocalService;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    configurationPid = CONFIG_PID,
    service = OurFilesExpirationTask.class
)
public class OurFilesExpirationTask
    extends BaseMessageListener {

  @Override
  protected void doReceive(Message message)
      throws Exception {

    if (_log.isDebugEnabled()) {
      _log.debug("Job to clean expired links is triggered.");
    }

    if (_config.formId() > 0) {
      final ChronoUnit expirationUnit = getChronoUnitFromConfig();
      if (Validator.isNull(expirationUnit)) {
        throw new RuntimeException(
            "Expiration unit is null. Verify Our Files configuration under Control Panel > System Settings > Third Party > Our Files");
      }
      List<DDMFormInstanceRecord> ourFilesRecords =
          _recordService.getFormInstanceRecords(_config.formId());
      ourFilesRecords.stream().filter(record -> {
        final LocalDateTime createDate = record.getCreateDate().toInstant()
                                               .atZone(ZoneId.systemDefault()).toLocalDateTime();
        final LocalDateTime expirationDate = createDate.plus(_config.expirationValue(),
                                                             expirationUnit);
        final LocalDateTime today = LocalDateTime.now(ZoneId.systemDefault());
        return today.isAfter(expirationDate);
      }).filter(record -> {
        try {
        Map<String, String> fields =
            _ourFilesFormService.getFieldsAsMap(record, StringPool.COMMA);
        JSONObject fileToShare =
            JSONFactoryUtil.createJSONObject(fields.get(FIELD_FILE_TO_SHARE));
        long fileEntryId = GetterUtil.getLong(fileToShare.get("fileEntryId"));
          DLFileEntry fileEntry = _fileEntryService.getDLFileEntry(fileEntryId);
          return Validator.isNotNull(fileEntry);
        } catch (NoSuchFileEntryException e) {
          return false;
        } catch (PortalException e) {
          _log.error(e.getLocalizedMessage(), e);
        }
        return false;
      }).forEach(record -> {
        if (_log.isDebugEnabled()) {
          _log.debug("Record ({}) is expired (createDate={},expiration={} {})",
                     record.getFormInstanceRecordId(),
                     record.getCreateDate(),
                     _config.expirationValue(),
                     _config.expirationUnit()

          );
        }
        try {
          Map<String, String> fields =
              _ourFilesFormService.getFieldsAsMap(record, StringPool.COMMA);
          JSONObject fileToShare =
              JSONFactoryUtil.createJSONObject(fields.get(FIELD_FILE_TO_SHARE));
          long fileEntryId = GetterUtil.getLong(fileToShare.get("fileEntryId"));
          try {
            _fileEntryService.deleteFileEntry(fileEntryId);
            if (_log.isDebugEnabled()) {
              _log.debug("Deleted fileEntry ({})", fileEntryId);
            }
          } catch (NoSuchFileEntryException e) {
            _log.warn(e.getLocalizedMessage());
          }
        } catch (PortalException e) {
          _log.error(e.getLocalizedMessage(), e);
        }
      });
    }
  }

  private ChronoUnit getChronoUnitFromConfig() {

    switch (_config.expirationUnit()) {
      case "second":
        return ChronoUnit.SECONDS;
      case "minute":
        return ChronoUnit.MINUTES;
      case "hour":
        return ChronoUnit.HOURS;
      case "day":
        return ChronoUnit.DAYS;
      case "week":
        return ChronoUnit.WEEKS;
      case "month":
        return ChronoUnit.MONTHS;
      case "year":
        return ChronoUnit.YEARS;
    }

    return null;
  }

  @Activate
  @Modified
  protected void activate(Map<String, Object> properties) {

    _config = ConfigurableUtil.createConfigurable(OurFilesConfiguration.class, properties);
    TimeUnit timeUnit = TimeUnit.valueOf(_config.frequency().toUpperCase());

    Class<?> clazz = getClass();
    String className = clazz.getName();

    Trigger trigger = _triggerFactory
        .createTrigger(className, className,
                       null, null,
                       _config.interval(), timeUnit);

    SchedulerEntryImpl schedulerEntry = new SchedulerEntryImpl(className, trigger);

    _schedulerEngineHelper
        .register(this, schedulerEntry,
                  DestinationNames.SCHEDULER_DISPATCH);
  }

  @Deactivate
  protected void deactivate() {

    _schedulerEngineHelper.unregister(this);
  }

  @Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED)
  protected ModuleServiceLifecycle _moduleServiceLifecycle;

  @Reference
  protected SchedulerEngineHelper _schedulerEngineHelper;

  @Reference
  protected TriggerFactory _triggerFactory;

  @Reference
  private DDMFormInstanceRecordLocalService _recordService;

  @Reference
  private DLFileEntryLocalService _fileEntryService;

  @Reference
  private OurFilesFormService _ourFilesFormService;

  private volatile OurFilesConfiguration _config;

  public static final Logger _log = LoggerFactory.getLogger(OurFilesExpirationTask.class);

}
