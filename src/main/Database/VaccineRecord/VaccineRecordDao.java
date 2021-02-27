package Database.VaccineRecord;

import Database.Dao;

import java.util.List;

public interface VaccineRecordDao extends Dao<VaccineRecord> {

  List<VaccineRecord> getAllBetween(long start, long end);

  List<VaccineRecord> getAllFromOrg(String orgName);
}
