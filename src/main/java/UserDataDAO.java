import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.validator.ValidatorException;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;

@Named
@ApplicationScoped
public class UserDataDAO {

    EntityManager entityManager;

    @Inject
    RegisterRequests registerRequest;

    public UserDataDAO() {
        this.entityManager = Persistence.createEntityManagerFactory("fallstudie").createEntityManager();
    }

    public void validateUsernameAndPassword(CurrentUser currentUser, String username, String password) {
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            // Erstellen des CriteriaBuilder und der Query
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Users> cq = cb.createQuery(Users.class);
            Root<Users> root = cq.from(Users.class);

            cq.select(root)
                    .where(cb.equal(root.get("username"), username));

            // Ausführen der Query und Speichern der Ergebnisse
            List<Users> usersList = entityManager.createQuery(cq).getResultList();

            // Prüfen, ob der Benutzer gefunden wurde
            if (usersList.isEmpty()) {
               FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User could not found.."));
                transaction.rollback();
                return;
            }

            Users user = usersList.get(0); // Get the first user

            // Password verifizierung mit BCrypt
            if (!BCrypt.checkpw(password, user.getPassword())) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Incorrect Password."));
                transaction.rollback();
                return;
            }

            // Setzte den current user
            currentUser.setUsername(user.getUsername());
            currentUser.setRole(user.getRole());

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            System.out.println("Fehler bei der Benutzervalidierung: " + e.getMessage());
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Something went wrong."));
        }
}

    public void registerUser(String username, String password) {
    EntityTransaction transaction = entityManager.getTransaction();
    try {
        transaction.begin();

        // Hash the password using BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        registerRequest.setUsername(username);
        registerRequest.setPassword(hashedPassword);

        entityManager.persist(registerRequest);

        transaction.commit();
        System.out.println("User registered successfully: " + username);
    } catch (Exception e) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
        System.out.println("Error registering user: " + e.getMessage());
        throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User registration failed."));
    }
}

    public boolean userExists(String username) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            // Create the CriteriaBuilder and the query
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Users> root = cq.from(Users.class);

            // Query to count users with the given username
            cq.select(cb.count(root))
                    .where(cb.equal(root.get("username"), username));

            // Execute the query and get the result
            Long count = entityManager.createQuery(cq).getSingleResult();
            transaction.commit();

            System.out.println("User exists: " + count);

            // Return true if count is greater than 0, otherwise false
            return count > 0;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            System.out.println("Error checking if user exists: " + e.getMessage());
            return false;
        }
    }
}