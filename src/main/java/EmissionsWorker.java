import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

@Named
@RequestScoped
public class EmissionsWorker implements Serializable {

    private Map<String, Map<Integer, BigDecimal>> pivotedCo2Data;
    private List<Integer> years;
    private List<Map.Entry<String, Map<Integer, BigDecimal>>> filteredPivotedCo2Data;
    private LineChartModel top5CountriesModel;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private String searchCountry;

    private Integer countryId;
    private String countryName;
    private Integer year;
    private BigDecimal emission;

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getEmission() {
        return emission;
    }

    public void setEmission(BigDecimal emissions) {
        this.emission = emissions;
    }

    private List<Country> countries;

    @Inject
    private CountryDAO countryDAO;

    @Inject
    private EmissionsDAO emissionsDAO;

    @PostConstruct
    public void loadData() {
        System.out.println("Start loadData()");
        try {
            pivotedCo2Data = emissionsDAO.getPivotedCo2Data();
            years = pivotedCo2Data.values().stream()
                    .flatMap(map -> map.keySet().stream())
                    .distinct()
                    .sorted()
                    .toList();

            filteredPivotedCo2Data = new ArrayList<>(pivotedCo2Data.entrySet());

            createTop5CountriesModel();
            System.out.println("Data loaded successfully");

            loadCountries();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in loadData: " + e.getMessage());
        }
    }

    private void loadCountries() {
        countries = countryDAO.getAllCountries();
    }

    public boolean validateYear(Integer year) {
        return year > 0 && year <= Calendar.getInstance().get(Calendar.YEAR);
    }

    public void saveEmission() {
        if (!validateYear(year)) {
            System.out.println("Error: The year cannot be in the future.");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "The year cannot be in the future.");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return;
        }

        emissionsDAO.saveEmission(countryId, countryName, year, emission);

        countryId = null;
        countryName = null;
        year = null;
        emission = null;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    private void createTop5CountriesModel() {
        top5CountriesModel = new LineChartModel();
        Map<String, BigDecimal> totalEmissions = new HashMap<>();

        // Berechnet die Gesamtemissionen für jedes Land
        for (Map.Entry<String, Map<Integer, BigDecimal>> entry : pivotedCo2Data.entrySet()) {
            String country = entry.getKey();
            BigDecimal total = entry.getValue().values().stream()
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalEmissions.put(country, total);
        }

        // Ermittelt die Top-5-Länder mit den höchsten Gesamtemissionen
        List<Map.Entry<String, BigDecimal>> top5 = totalEmissions.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Bestimmt die Jahr-Spanne, die im Diagramm dargestellt werden soll
        int earliestYear = pivotedCo2Data.values().stream()
                .flatMap(map -> map.keySet().stream())
                .min(Integer::compareTo)
                .orElse(0);

        int latestYear = pivotedCo2Data.values().stream()
                .flatMap(map -> map.keySet().stream())
                .max(Integer::compareTo)
                .orElse(0);

        // Erstellt die Linien-Datenreihen für jedes der Top-5-Länder
        for (Map.Entry<String, BigDecimal> entry : top5) {
            String country = entry.getKey();
            LineChartSeries series = new LineChartSeries();
            series.setLabel(country);

            // Füllt die Datenreihe mit `null` für fehlende Jahre
            Map<Integer, BigDecimal> countryData = pivotedCo2Data.get(country);
            for (int year = earliestYear; year <= latestYear; year++) {
                BigDecimal emission = countryData.getOrDefault(year, null);
                series.set(year, emission); // Jahre ohne Daten haben `null`
            }

            top5CountriesModel.addSeries(series);
        }

        top5CountriesModel.setLegendPosition("e");
    }

    public BigDecimal getTotalEmissionsValue(String country) {
        BigDecimal total = pivotedCo2Data.get(country).values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total != null ? total : BigDecimal.ZERO;
    }

    public String getTotalEmissionsFormatted(String country) {
        BigDecimal total = getTotalEmissionsValue(country);
        return DECIMAL_FORMAT.format(total);
    }


    public List<Integer> getYears() {
        return years != null ? years : Collections.emptyList();
    }

    public List<Map.Entry<String, Map<Integer, BigDecimal>>> getFilteredPivotedCo2Data() {
        return filteredPivotedCo2Data != null ? filteredPivotedCo2Data : Collections.emptyList();
    }

    public LineChartModel getTop5CountriesModel() {
        return top5CountriesModel;
    }

    public void setFilteredPivotedCo2Data(List<Map.Entry<String, Map<Integer, BigDecimal>>> filteredPivotedCo2Data) {
        this.filteredPivotedCo2Data = filteredPivotedCo2Data;
    }

    public void setYears(List<Integer> years) {
        this.years = years;
    }

    public List<Map.Entry<String, Map<Integer, BigDecimal>>> getSearchedEmissions() {
        if (searchCountry != null && !searchCountry.isEmpty()) {
            if (searchCountry.equals("@all")) {
                return new ArrayList<>(pivotedCo2Data.entrySet());
            }
            List<Map.Entry<String, Map<Integer, BigDecimal>>> results = pivotedCo2Data.entrySet().stream()
                    .filter(entry -> entry.getKey().toLowerCase().contains(searchCountry.toLowerCase()))
                    .collect(Collectors.toList());
            if (results.isEmpty()) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No data available for the specified country.");
                FacesContext.getCurrentInstance().addMessage(null, message);
            }
            return results;
        }
        return Collections.emptyList();
    }

    public void filterByCountry() {
        System.out.println("Filter by country: " + searchCountry);
        getSearchedEmissions();
    }

    public String getSearchCountry() {
        return searchCountry;
    }

    public void setSearchCountry(String searchCountry) {
        this.searchCountry = searchCountry;
    }

    public void updateCountryName() {
        for (Country country : countries) {
            if (country.getId().equals(countryId)) {
                countryName = country.getName();
                break;
            }
        }
    }
}