import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

@Named
@ApplicationScoped
public class RegisterRequestDAO {

    @Inject
    Users newUser;

    private EntityManagerFactory emf = Persistence.createEntityManagerFactory("fallstudie");

    public int countPendingRegisterRequests() {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(r) FROM RegisterRequests r");
            Long count = (Long) query.getSingleResult();
            return count.intValue();
        } finally {
            em.close();
        }
    }

    public List<RegisterRequests> getAllPendingRegisterRequests() {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery("SELECT r FROM RegisterRequests r");
            System.out.println("RegisterRequestDAO: " + query.getResultList());
            return query.getResultList(); // Liste aller Registrierungsanfragen zurückgeben
        } finally {
            em.close();
        }
    }

    // Methode zum Akzeptieren einer Registrierungsanfrage: fügt Benutzer hinzu und entfernt den Antrag
    public void acceptRequest(RegisterRequests registerRequest) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();

            // Check if the username already exists
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Users> root = cq.from(Users.class);
            cq.select(cb.count(root)).where(cb.equal(root.get("username"), registerRequest.getUsername()));
            Long count = em.createQuery(cq).getSingleResult();

            if (count > 0) {
                System.out.println("Username already exists: " + registerRequest.getUsername());
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Username already exists!"));
                transaction.rollback();
                return;
            }

            // Erstellen eines neuen Users mit den Informationen des RegisterRequests
            newUser.setUsername(registerRequest.getUsername());
            newUser.setPassword(registerRequest.getPassword());
            newUser.setRole(0); // Rolle für neuen Benutzer auf 0 setzen

            System.out.println("newUser: " + newUser.getUsername());
            System.out.println("newUser: " + newUser.getPassword());
            System.out.println("newUser: " + newUser.getRole());

            // Speichern des neuen Benutzers in der Users-Tabelle
            em.persist(newUser);

            // Entfernen des RegisterRequests aus der RegisterRequest-Tabelle
            RegisterRequests managedRequest = em.find(RegisterRequests.class, registerRequest.getId());
            if (managedRequest != null) {
                em.remove(managedRequest);
            }

            transaction.commit();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Registration request accepted successfully!"));
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
    // Methode zum Ablehnen einer Registrierungsanfrage: entfernt nur den Antrag
    public void declineRequest(RegisterRequests request) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            RegisterRequests reqToRemove = em.find(RegisterRequests.class, request.getId());
            if (reqToRemove != null) {
                em.remove(reqToRemove); // Antrag entfernen
            }
            em.getTransaction().commit();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Registration request declined successfully!"));
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}