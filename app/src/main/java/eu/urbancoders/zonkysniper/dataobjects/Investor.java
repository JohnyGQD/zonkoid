package eu.urbancoders.zonkysniper.dataobjects;

/**
 * Informace o investorovi
 *
 * Author: Ondrej Steger (ondrej@steger.cz)
 * Date: 23.10.2016
 */

public class Investor {

    /**
     * {
     * "id":13356,
     * "username":"ondrej.steger@gmail.com",
     * "email":"ondrej.steger@gmail.com",
     * "firstName":"Ondřej",
     * "surname":"Steger",
     * "nickName":"kath",
     * "phone":"777929442",
     * "bankAccount":{
     *      "id":2864,
     *      "accountNo":"0001150191040207",
     *      "accountBank":"0100",
     *      "accountName":null
     *      },
     * "permanentAddress":{
     *      "street":"U Lesního divadla",
     *      "streetNo":"38",
     *      "city":"Liberec",
     *      "zipCode":"46014",
     *      "country":"CZ",
     *      "yearFromLive":null
     *      },
     * "contactAddress":{
     *      "street":null,
     *      "streetNo":null,
     *      "city":null,
     *      "zipCode":null,
     *      "country":"CZ",
     *      "yearFromLive":null
     *      },
     * "roles":["SCOPE_APP_WEB","ROLE_SECURED_USER","ROLE_INVESTOR"],
     * "unreadNotificationsCount":0,
     * "showNotificationSettings":false,
     * "status":"ACTIVE",
     * "dateRegistered":"2016-02-22T16:10:29.464+01:00",
     * "dateLastLoggedIn":"2016-10-23T22:08:30.924+02:00",
     * "daysSinceLastLogin":0,
     * "userMarketing":"webReg",
     * "webId":"67be5282-0728-4a59-9070-7fe3c755e2e9"
     * }
     */
    int id;
    String username;
    String email;
    String firstName;
    String surname;
    String nickName;
    int unreadNotificationsCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }
}