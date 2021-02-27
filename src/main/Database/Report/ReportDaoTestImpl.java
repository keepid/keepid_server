package Database.Report;

import Issue.IssueReport;
import com.google.inject.Inject;
import org.bson.types.ObjectId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReportDaoTestImpl implements ReportDao {
  private Map<String, IssueReport> issueMap;

  @Inject
  public ReportDaoTestImpl() {
    this.issueMap = new LinkedHashMap<>();
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
