import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
public class Emissions implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false)
    private Country country;

    @Column
    private Integer year;

    @Column
    private BigDecimal emission;

    public Emissions() {
    }

    // Getter und Setter

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public BigDecimal getEmission() {
        return emission;
    }

    public void setEmission(BigDecimal emission) {
        this.emission = emission;
    }
}