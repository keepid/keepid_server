package Database.Report;

import Database.Dao;
import Issue.IssueReport;

import java.util.Optional;

public interface ReportDao extends Dao<IssueReport> {

  Optional<IssueReport> get(String title);
}
