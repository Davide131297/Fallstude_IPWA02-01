import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.primefaces.PrimeFaces;
import java.util.List;

@Named
@ApplicationScoped
public class RequestUpdateDataDAO {

    private EntityManagerFactory emf = Persistence.createEntityManagerFactory("fallstudie");
    private RequestUpdateData pendingRequestUpdateData;

    public List<RegisterRequests> getAllPendingUpdateRequests() {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery("SELECT r FROM RequestUpdateData r");
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    // Methode zum Akzeptieren einer Updateanfrage: fügt Benutzer hinzu und entfernt den Antrag
    public void acceptRequest(RequestUpdateData requestUpdateData) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();

            // Das Country-Objekt mithilfe der ID des Landes finden
            Country country = em.find(Country.class, requestUpdateData.getCountry_id());

            // Überprüfen, ob der Datensatz bereits existiert
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<Emissions> emissionsRoot = query.from(Emissions.class);

            query.select(cb.count(emissionsRoot))
                    .where(
                            cb.equal(emissionsRoot.get("country"), country),
                            cb.equal(emissionsRoot.get("year"), requestUpdateData.getYear())
                    );

            Long count = em.createQuery(query).getSingleResult();

            if (count > 0) {
                pendingRequestUpdateData = requestUpdateData;
                PrimeFaces.current().executeScript("PF('confirmUpdateDialog').show()");
                transaction.rollback();
                return;
            }

            // Datensatz existiert nicht - neuer Emissionsdatensatz wird erstellt
            Emissions newEmissionData = new Emissions();
            newEmissionData.setCountry(country);
            newEmissionData.setYear(requestUpdateData.getYear());
            newEmissionData.setEmission(requestUpdateData.getEmissions());

            // Speichern des neuen Datensatzes in der Emissions-Tabelle
            em.persist(newEmissionData);

            // Entfernen des UpdateRequests aus der RequestUpdateData-Tabelle
            RequestUpdateData managedRequest = em.find(RequestUpdateData.class, requestUpdateData.getId());
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
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred during processing!"));
        } finally {
            em.close();
        }
    }

    public void updateData() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();

            // Land anhand des Namens finden
            Country country = em.createQuery("SELECT c FROM Country c WHERE c.name = :name", Country.class)
                    .setParameter("name", pendingRequestUpdateData.getCountry_name())
                    .getSingleResult();


            // Update des Emissionsdatensatzes
            Emissions existingEmission = em.createQuery("SELECT e FROM Emissions e WHERE e.country = :country AND e.year = :year", Emissions.class)
                    .setParameter("country", country)
                    .setParameter("year", pendingRequestUpdateData.getYear())
                    .getSingleResult();

            existingEmission.setEmission(pendingRequestUpdateData.getEmissions());
            em.merge(existingEmission);

            // Entfernen des UpdateRequests aus der RequestUpdateData-Tabelle
            RequestUpdateData requestToRemove = em.createQuery("SELECT r FROM RequestUpdateData r WHERE r.country_name = :countryName AND r.year = :year AND r.emissions = :emissions", RequestUpdateData.class)
                    .setParameter("countryName", pendingRequestUpdateData.getCountry_name())
                    .setParameter("year", pendingRequestUpdateData.getYear())
                    .setParameter("emissions", pendingRequestUpdateData.getEmissions())
                    .getSingleResult();

            if (requestToRemove != null) {
                em.remove(requestToRemove);
            }

            transaction.commit();
            pendingRequestUpdateData = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Data updated successfully!"));
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred during processing!"));
        } finally {
            em.close();
        }
    }

    // Methode zum Ablehnen einer Updateanfrage: entfernt nur den Antrag
    public void declineRequest(RequestUpdateData request) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            RequestUpdateData reqToRemove = em.find(RequestUpdateData.class, request.getId());
            if (reqToRemove != null) {
                em.remove(reqToRemove); // Antrag entfernen
            }
            em.getTransaction().commit();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Update request declined successfully!"));
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public long getPendingRequestsCount() {
        List<RegisterRequests> pendingRequests = getAllPendingUpdateRequests();
        System.out.println("getPendingRequestsCount: " + pendingRequests.size());
        return pendingRequests.size();
    }

    public List<RegisterRequests> getAllPendingRequests() {
        return getAllPendingUpdateRequests();
    }

}