import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Named
@ApplicationScoped
public class EmissionsDAO {

    EntityManager entityManager;
    CriteriaBuilder criteriaBuilder;

    @Inject
    CurrentUser currentUser;

    @PostConstruct
    public void init() {
        try {
            entityManager = Persistence.createEntityManagerFactory("fallstudie").createEntityManager();
            criteriaBuilder = entityManager.getCriteriaBuilder();

            // Initialisierung der Daten, wenn die Tabelle leer ist
            long count = getCo2DataCount();
            System.err.println("Wir haben " + count + " CO₂-Daten.");

            if (count == 0) {
                System.err.println("Initialisierung der Daten.");
                EntityTransaction transaction = getAndBeginTransaction();
                // Hier könntest du Initialdaten einfügen, falls nötig
                transaction.commit();
            } else {
                System.err.println("Datenbank enthält bereits Daten.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public long getCo2DataCount() {
        CriteriaQuery<Long> cq = criteriaBuilder.createQuery(Long.class);
        cq.select(criteriaBuilder.count(cq.from(Emissions.class)));
        return entityManager.createQuery(cq).getSingleResult();
    }

    public List<Emissions> getAllCo2Data() {
        CriteriaQuery<Emissions> cq = criteriaBuilder.createQuery(Emissions.class);
        cq.from(Emissions.class);
        return entityManager.createQuery(cq).getResultList();
    }

    // Daten holen und pivotieren
    public Map<String, Map<Integer, BigDecimal>> getPivotedCo2Data() {
        List<Emissions> co2DataList = getAllCo2Data();
        Map<String, Map<Integer, BigDecimal>> pivotedData = new TreeMap<>(); // Verwenden einer TreeMap für die Sortierung

        // Pivotierte Datenstruktur aufbauen
        for (Emissions data : co2DataList) {
            String countryName = data.getCountry().getName();
            int year = data.getYear();

            // Überprüfen, ob die Emissionen null sind
            BigDecimal emission = data.getEmission();
            if (emission != null) {
                pivotedData.putIfAbsent(countryName, new HashMap<>());
                pivotedData.get(countryName).put(year, emission);
            } else {
                System.out.println("Null emission value for country: " + countryName + " in year: " + year);
            }
        }

        return pivotedData;
    }

    public EntityTransaction getAndBeginTransaction() {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        return transaction;
    }

    public void saveEmission(int countryId, String countryName, int year, BigDecimal emissions) {
        System.err.println("Saving emission: " + countryName + ", " + year + ", " + emissions);
        EntityTransaction transaction = getAndBeginTransaction();
        try {
            RequestUpdateData requestData = new RequestUpdateData();
            requestData.setCountry_id(countryId);
            requestData.setCountry_name(countryName);
            requestData.setYear(year);
            requestData.setEmissions(emissions);
            requestData.setFromUser(currentUser.getUsername());
            entityManager.persist(requestData);
            transaction.commit();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Request Sent", "Your request to update the data has been successfully submitted.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to save data.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }
}