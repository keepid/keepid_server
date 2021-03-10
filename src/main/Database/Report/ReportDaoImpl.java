package Database.Report;

import Database.MongoConfig;
import Issue.IssueReport;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public class ReportDaoImpl implements ReportDao {
  private MongoCollection<IssueReport> issueReportMongoCollection;

  @Inject
  public ReportDaoImpl(MongoConfig mongoConfig) {
    this.issueReportMongoCollection =
        mongoConfig.getDatabase().getCollection("IssueReport", IssueReport.class);
  }

  @Override
  public Optional<IssueReport> get(ObjectId id) {
    return null;
  }

  @Override
  public Optional<IssueReport> get(String title) {
    return null;
  }

  @Override
  public List<IssueReport> getAll() {
    return null;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public void update(IssueReport issueReport) {}

  @Override
  public void save(IssueReport issueReport) {}

  @Override
  public void delete(IssueReport issueReport) {}

  @Override
  public void clear() {}
}
