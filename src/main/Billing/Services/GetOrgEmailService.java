package Billing.Services;

import Billing.BillingMessage;
import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Config.Service;
import Organization.Organization;
import com.stripe.exception.StripeException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.stripe.model.Product;

import static com.mongodb.client.model.Filters.eq;

public class GetOrgEmailService implements Service {
    final String orgName;
    private String orgEmail;
    private MongoCollection<Organization> orgCollection;

    public GetOrgEmailService(String orgName) {
        this.orgName = orgName;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (orgName == null || (orgName.strip()).equals("")){
            return BillingMessage.INVALID_ORG_NAME;
        }

        MongoDatabase db = null;

        try{
            db = MongoConfig.getDatabase(DeploymentLevel.STAGING);
        }
        catch (Exception e){
            return BillingMessage.DB_NULL;
        }

        orgCollection = db.getCollection("organization", Organization.class);

        Organization org = null;

        try{
            System.out.println("trying the code");
            org = orgCollection.find(eq("orgName", "randomOrg")).first();
            orgEmail = org.getOrgEmail();
        }
        catch (Exception e){
            System.out.println(e);
            return BillingMessage.INVALID_DB_ORGANIZATION_NAME;
        }

        System.out.println("broke out of try catch");

        // get orgEmail from database and return it
        orgEmail = org.getOrgEmail();

        return BillingMessage.SUCCESS;
    }

    public String getOrgEmail(){
        return orgEmail;
    }
}