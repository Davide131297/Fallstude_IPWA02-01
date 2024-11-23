import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class RequestUpdateData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int country_id;

    @Column(nullable = false)
    private String country_name;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private BigDecimal emissions;

    @Column(nullable = false)
    private String fromUser;

    // Getters und Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCountry_id() {
        return country_id;
    }

    public void setCountry_id(int country_id) {
        this.country_id = country_id;
    }

    public String getCountry_name() {
        return country_name;
    }

    public void setCountry_name(String country) {
        this.country_name = country;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getEmissions() {
        return emissions;
    }

    public void setEmissions(BigDecimal emissions) {
        this.emissions = emissions;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }
}