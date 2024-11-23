import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import java.io.Serializable;

@Named
@ViewScoped
public class RegisterController implements Serializable {

    @Inject
    private UserDataDAO userData;

    private String username;
    private String password;
    private String passwordRepeat;

    // Getter und Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordRepeat() { return passwordRepeat; }
    public void setPasswordRepeat(String passwordRepeat) { this.passwordRepeat = passwordRepeat; }

    // Registrierungsmethode
    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();


        if (username == null || username.isEmpty()) {
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username is empty", "Please enter a username."));
            return "";
        } else if (username.length() < 4) {
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username too short", "Username must be at least 4 characters long."));
            return "";
        }

        if (!password.equals(passwordRepeat)) {
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Passwords do not match", "Please check your passwords."));
            return "";
        }

        if (!isValidPassword(password)) {
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid password format", "Password must contain uppercase, lowercase letters, and digits, and be at least 4 characters long."));
            return "";
        }

        try {
            // Prüfen ob der Benutzer bereits existiert
            if (userData.userExists(username)) {
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "User already exists", "Please choose a different username."));
                return "";
            } else {
                userData.registerUser(username, password);
                return "registerSuccess.xhtml?faces-redirect=true";
            }
        } catch (Exception e) {
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while checking the username."));
            return "";
        }
    }

    // Password format validation method
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{4,}$";
        return password != null && password.matches(regex);
    }

    public String goBack() {
        return "login.xhtml?faces-redirect=true";
    }
}