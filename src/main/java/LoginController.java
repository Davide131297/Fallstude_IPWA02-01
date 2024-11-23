import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;

@Named
@ViewScoped
public class LoginController implements Serializable {

    @Inject
    UserDataDAO userData;

    @Inject
    CurrentUser currentUser;

    String username, password;
    String tempUsername;
    Integer role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void postValidateUser(ComponentSystemEvent ev) {
        UIInput temp = (UIInput) ev.getComponent();
        System.out.println("PostValidateUser wird ausgeführt..." + temp.getValue());
        tempUsername = (String) temp.getValue();
        if (tempUsername == null || tempUsername.trim().isEmpty()) {
            temp.setValid(false);
            addMessage("Error", FacesMessage.SEVERITY_ERROR, "You need to enter a username.");
        }
    }

    public void validateLogin(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String password = (String) value;

        if (tempUsername != null && !tempUsername.isEmpty()) {
            userData.validateUsernameAndPassword(currentUser, tempUsername, password);
            System.out.println("Login wird ausgeführt...");
        }
    }

    public String login() {

        if (password == null || password.isEmpty()) {
            addMessage("Missed Password", FacesMessage.SEVERITY_ERROR, "You need to enter a password.");
            return ""; // Bleibe auf der Login-Seite
        }

        role = currentUser.getRole();
        if (role == null) {
            return ""; // Bleibe auf der Login-Seite
        }

        // Bestimme die Rolle des Benutzers
        if (role == 0) {
            return "scientistPage.xhtml?faces-redirect=true";
        } else if (role == 1) {
            return "publisherPage.xhtml?faces-redirect=true";
        } else {
            System.out.println("Login fehlgeschlagen.");
            return "";
        }
    }

    private void addMessage(String summary, FacesMessage.Severity severity, String detail) {
        FacesMessage message = new FacesMessage(severity, summary, detail);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public String logout() {
        currentUser.reset();
        return "index.xhtml?faces-redirect=true";
    }

    public String goBack() {
        return "index.xhtml?faces-redirect=true";
    }

    public String goToRegister() {
        return "register.xhtml?faces-redirect=true";
    }

    public void checkLoginPublisher() {
        Integer role = currentUser.getRole();
        if (role == null || role != 1) {
            FacesContext fc = FacesContext.getCurrentInstance();
            NavigationHandler nh = fc.getApplication().getNavigationHandler();
            nh.handleNavigation(fc, null, "login.xhtml?faces-redirect=true");
        }
    }

    public void checkLoginScientist() {
        Integer role = currentUser.getRole();
        if (role == null || role != 0) {
            FacesContext fc = FacesContext.getCurrentInstance();
            NavigationHandler nh = fc.getApplication().getNavigationHandler();
            nh.handleNavigation(fc, null, "login.xhtml?faces-redirect=true");
        }
    }
}