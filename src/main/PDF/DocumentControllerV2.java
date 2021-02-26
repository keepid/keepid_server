package PDF;

import com.google.inject.Inject;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;

public class DocumentControllerV2 implements CrudHandler {

  @Inject
  public DocumentControllerV2() {}

  public void getAll(Context context) {
    context.result("Not Implemented");
  }

  public void getOne(Context context, String docId) {
    context.result("Not Implemented");
  }

  public void create(Context context) {
    context.result("Not Implemented");
  }

  public void update(Context context, String docId) {
    context.result("Not Implemented");
  }

  public void delete(Context context, String docId) {
    context.result("Not Implemented");
  }
}
