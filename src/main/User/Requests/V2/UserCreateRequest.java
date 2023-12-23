package User.Requests.V2;

import User.UserType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;


public class UserCreateRequest
{
    @BsonProperty(value = "firstName")
    @JsonProperty("firstName")
    private String firstName;

    @BsonProperty(value = "lastName")
    @JsonProperty("lastName")
    private String lastName;

    @BsonProperty(value = "email")
    @JsonProperty("email")
    private String email;

    @BsonProperty(value = "privilegeLevel")
    @JsonProperty("privilegeLevel")
    private UserType userType;

    @BsonProperty(value = "organization")
    private String organization;

    @BsonProperty(value = "username")
    private String username;

    @BsonProperty(value = "password")
    private String password;


    /** **************** GETTERS ********************* */
    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getEmail() { return email; }

    public UserType getUserType() { return userType; }

    public String getOrganization() { return organization; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    /** **************** SETTERS ********************* */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setOrganization(String organization) { this.organization = organization; }

    public void setUserType(UserType userType) { this.userType = userType; }

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

}