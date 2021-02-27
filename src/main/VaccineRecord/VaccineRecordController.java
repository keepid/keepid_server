package VaccineRecord;

import Database.VaccineRecord.VaccineRecord;
import Database.VaccineRecord.VaccineRecordDao;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Date;

@Slf4j
public class VaccineRecordController {

  private VaccineRecordDao recordDao;

  public VaccineRecordController(VaccineRecordDao recordDao) {
    this.recordDao = recordDao;
  }

  public Handler recordDose =
      ctx -> {
        DoseRequest doseRequest = ctx.bodyAsClass(DoseRequest.class);
        log.debug("Got a dose request {}", doseRequest.toString());

        Date dateOfDose = doseRequest.getDate();
        long dateTimeOfNextDose = dateOfDose.getTime() + 8 * 24 * 60 * 60 * 1000;
        Date dateOfNextDose = new Date(dateTimeOfNextDose);
        VaccineRecord record =
            VaccineRecord.builder()
                .userId(doseRequest.getUserId())
                .orgName(doseRequest.getOrgName())
                .provider(doseRequest.getProvider())
                .manufacturer(doseRequest.getManufacturer())
                .dose(doseRequest.getDose())
                .dateOfDose(doseRequest.getDate().toInstant().toEpochMilli())
                .dateOfNextDose(dateOfNextDose.toInstant().toEpochMilli())
                .build();

        recordDao.save(record);
      };

  public Handler notify =
      ctx -> {
        long startDate = Instant.now().toEpochMilli();
        long endDate = startDate + 8 * 24 * 60 * 60 * 1000;
        log.info(
            "Notifying all records expiring between {} and {}",
            new Date(startDate).toString(),
            new Date(endDate).toString());
      };

  public Handler getDosesFromOrg =
      ctx -> {
        String orgName = ctx.pathParam("orgName");
        orgName = orgName.replace('+', ' ');
        log.info("Getting doses from {}", orgName);

        JSONObject result = new JSONObject();

        result.put("orgs", recordDao.getAllFromOrg(orgName));
        ctx.result(result.toString());
      };
}
