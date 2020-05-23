package AccountSecurity;

import User.UserMessage;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.Handler;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Date;

import static com.mongodb.client.model.Filters.eq;

public class AccountSecurityController {
  MongoDatabase db;

  public AccountSecurityController(MongoDatabase db) {
    this.db = db;
  }

  public Handler forgotPassword =
      ctx -> {
        long expirationTime = 7200000; // 2 hours
        JSONObject req = new JSONObject(ctx.body());
        String username = req.getString("username");
        MongoCollection<Document> userCollection = db.getCollection("user");
        MongoCollection<Document> linkCollection = db.getCollection("link");
        Document user = userCollection.find(eq("username", username)).first();
        if (user == null) {
          ctx.json(UserMessage.USER_NOT_FOUND.toJSON());
        } else {
          String email = user.get("email", String.class);
          if (email == null) {
            ctx.json(UserMessage.INVALID_PARAMETER.toJSON("Email not found on this user"));
          } else {
            String id;
            do {
              id = RandomStringUtils.random(25, 48, 122, true, true, null, new SecureRandom());
            } while (linkCollection.find(eq("id", id)).first() != null);
            String link =
                CreateResetLink.createJWT(
                    id, "KeepID", username, "Password Reset Confirmation", expirationTime);
            EmailUtil.sendEmail(
                "mail.privateemail.com",
                "587",
                "contact@keep.id",
                "Keep Id",
                "keepid2020", // change later to get actual password from config file
                email,
                "Password Reset Confirmation",
                "https://keep.id/reset-password/" + link);
            ctx.json(UserMessage.SUCCESS.toJSON());
          }
        }
      };

  public Handler changePasswordIn =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String oldPassword = req.getString("oldPassword");
        String newPassword = req.getString("newPassword");
        String username = ctx.sessionAttribute("username");
        JSONObject res = new JSONObject();
        if (change(username, newPassword, oldPassword, db)) {
          res.put("status", UserMessage.SUCCESS.toJSON());
        } else {
          res.put("status", UserMessage.AUTH_FAILURE.toJSON());
        }
        ctx.json(res);
      };

  public Handler resetPassword =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String jwt = ctx.pathParam("jwt");
        Claims claim = CreateResetLink.decodeJWT(jwt);
        // Check if everything is valid exp user id (maybe set up hash map of seen)
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        String id = claim.getId();
        MongoCollection<Document> userCollection = db.getCollection("user");
        MongoCollection<Document> resetIDs = db.getCollection("emailIDs");
        Document user = userCollection.find(eq("username", claim.getAudience())).first();
        Document resetID = resetIDs.find(Filters.eq("id", id)).first();
        JSONObject res = new JSONObject();
        if (!(claim.getExpiration().compareTo(now) < 0 || user == null || resetID != null)) {
          Document newID = new Document("id", id).append("expiration", claim.getExpiration());
          resetIDs.insertOne(newID);
          String newPassword = req.getString("newPassword");
          reset(claim.getAudience(), newPassword, db);
          res.put("status", "success");
          ctx.json(res);
        }
      };

  public static boolean change(
      String username, String newPassword, String oldPassword, MongoDatabase db) {
    Argon2 argon2 = Argon2Factory.create();
    MongoCollection<Document> userCollection = db.getCollection("user");
    Document user = userCollection.find(eq("username", username)).first();
    if (user == null) {
      return false;
    }
    char[] oldPasswordArr = oldPassword.toCharArray();
    char[] newPasswordArr = newPassword.toCharArray();
    String hash = user.get("password", String.class);
    if (!argon2.verify(hash, oldPasswordArr)) {
      argon2.wipeArray(oldPasswordArr);
      argon2.wipeArray(newPasswordArr);
      return false;
    }
    argon2.wipeArray(newPasswordArr);
    reset(username, newPassword, db);
    return true;
  }

  private static void reset(String username, String newPassword, MongoDatabase db) {
    Argon2 argon2 = Argon2Factory.create();
    MongoCollection<Document> userCollection = db.getCollection("user");
    Document user = userCollection.find(eq("username", username)).first();
    char[] newPasswordArr = newPassword.toCharArray();
    String passwordHash = argon2.hash(10, 65536, 1, newPasswordArr);
    Document query = new Document();
    query.append("_id", user.get("_id"));
    Document setData = new Document();
    setData.append("password", passwordHash);
    Document update = new Document();
    update.append("$set", setData);
    userCollection.updateOne(query, update);
    argon2.wipeArray(newPasswordArr);
  }
}