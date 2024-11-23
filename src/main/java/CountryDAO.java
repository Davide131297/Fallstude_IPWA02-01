import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import java.util.List;

@Named
@ApplicationScoped
public class CountryDAO {

    EntityManager entityManager;

    public CountryDAO() {
        this.entityManager = Persistence.createEntityManagerFactory("fallstudie").createEntityManager();
    }

    public List<Country> getAllCountries() {
        return entityManager.createQuery("SELECT c FROM Country c", Country.class).getResultList();
    }
}