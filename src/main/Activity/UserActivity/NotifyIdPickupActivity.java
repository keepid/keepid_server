package Activity.UserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class NotifyIdPickupActivity extends UserActivity {
    @Override
    public List<String> construct() {
        List<String> a = new ArrayList<>();
        a.add(Activity.class.getSimpleName());
        a.add(UserActivity.class.getSimpleName());
        a.add(NotifyIdPickupActivity.class.getSimpleName());
        return a;
    }

    public NotifyIdPickupActivity(
            String workerUsername, String clientUsername, String idToPickup) {
        super(workerUsername, clientUsername, idToPickup);
    }
}
