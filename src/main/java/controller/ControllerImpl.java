package controller;

import dto.CategoryHighlightDTO;
import dto.MeasurementDTO;
import dto.ProductDTO;
import dto.ProductHighlightDTO;
import dto.ProductStatsDTO;
import dto.Top10AppearanceDTO;
import dto.YearDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Υλοποίηση του IController.
 */
public class ControllerImpl implements IController {

    // ====== ΕΣΩΤΕΡΙΚΕΣ ΔΟΜΕΣ ΜΝΗΜΗΣ ======

    // περιγραφή προϊόντος (όνομα, alias, κατηγορία)
    private static class ProductInfo {
        String alias;
        String fullName;
        String category;
    }

    // δεδομένα για ένα έτος
    private static class YearRecord {
        int year;
        Map<String, Double> prices = new LinkedHashMap<String, Double>();
        String top10AliasesRaw;
        String top10HeadlinesRaw;
    }

    // alias -> ProductInfo
    private final Map<String, ProductInfo> productsByAlias = new LinkedHashMap<String, ProductInfo>();

    // year -> YearRecord
    private final Map<Integer, YearRecord> yearsByYear = new TreeMap<Integer, YearRecord>();

    // σειρά των aliases όπως εμφανίζονται στις στήλες του data.tsv
    private final List<String> aliasColumnOrder = new ArrayList<String>();

    // ====== ΥΛΟΠΟΙΗΣΗ initializeFromIni ======

    @Override
    public int initializeFromIni(String iniPath, String delimiter) throws IOException {

        String dataFilePath = null;
        String metadataFilePath = null;

        // 1. Διαβάζουμε το .ini
        try (BufferedReader br = new BufferedReader(new FileReader(iniPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if (key.equalsIgnoreCase("dataFile")) {
                    dataFilePath = value;
                } else if (key.equalsIgnoreCase("metadataFile")) {
                    metadataFilePath = value;
                }
            }
        }

        if (dataFilePath == null || metadataFilePath == null) {
            throw new IOException("INI file missing dataFile or metadataFile");
        }

        // 2. Καθαρίζουμε ό,τι υπήρχε παλιά στη μνήμη
        clearAll();

        // 3. Φορτώνουμε metadata.tsv και data.tsv
        loadMetadata(metadataFilePath, delimiter);
        int rows = loadData(dataFilePath, delimiter);

        // 4. Επιστρέφουμε πόσες χρονιές φορτώθηκαν
        return rows;
    }

    // ====== ΥΛΟΠΟΙΗΣΗ loadFile ======

    @Override
    public void loadFile(String path, String delimiter) throws IOException {
        // Δεν χρησιμοποιεί .ini
        // Φορτώνει μόνο το data.tsv, χωρίς metadata.tsv

        clearAll();
        loadDataWithoutMetadata(path, delimiter);
    }

    // ====== ΑΠΟ ΕΔΩ ΚΑΙ ΚΑΤΩ: θα συμπληρώσουμε στα επόμενα βήματα ======

    @Override
    public List<YearDTO> listYears() {
        List<YearDTO> out = new ArrayList<YearDTO>();
        for (Integer y : yearsByYear.keySet()) {
            YearRecord rec = yearsByYear.get(y);
            out.add(buildYearDTO(rec));
        }
        return out;
    }

    @Override
    public List<ProductDTO> listProducts() {
        List<ProductDTO> out = new ArrayList<ProductDTO>();

        // Για κάθε προϊόν (alias) που ξέρουμε από τα metadata / data
        for (String alias : productsByAlias.keySet()) {
            // Παίρνουμε τα measurements που έχουμε αποθηκεύσει για αυτό το προϊόν
            List<MeasurementDTO> measurements = collectMeasurementsForProduct(
                alias,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            );

            // Δημιουργούμε το DTO όπως το ορίζει η κλάση ProductDTO
            ProductDTO dto = new ProductDTO(alias, measurements);

            // Προσθέτουμε στη λίστα
            out.add(dto);
        }

        return out;
    }


    @Override
    public YearDTO getYearMeasurements(int year) {
        YearRecord rec = yearsByYear.get(year);
        if (rec == null) return null;
        return buildYearDTO(rec);
    }

    @Override
    public ProductDTO getProductMeasurements(String productName) {
        // Βρίσκουμε πληροφορίες για το προϊόν
        ProductInfo info = productsByAlias.get(productName);
        if (info == null) {
            return null;
        }

        // Μαζεύουμε όλες τις μετρήσεις (όλες τις χρονιές)
        List<MeasurementDTO> measurements = collectMeasurementsForProduct(
            info.alias,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        );

        // Φτιάχνουμε το DTO με τον ΜΟΝΟ επιτρεπτό constructor
        ProductDTO dto = new ProductDTO(
            info.alias,        // αυτό είναι το "name" που περιμένει το ProductDTO
            measurements       // αυτή είναι η λίστα MeasurementDTO
        );

        return dto;
    }


    
    /*public ProductDTO filterProductMeasurements(String productName, int minYear, int maxYear) {

        // 1. προσπάθησε με alias (το κλειδί στον χάρτη)
        ProductInfo info = productsByAlias.get(productName);

        // 2. αν δεν βρέθηκε, προσπάθησε να το βρεις με fullName
        if (info == null) {
            for (ProductInfo pi : productsByAlias.values()) {
                if (pi.fullName != null && pi.fullName.equalsIgnoreCase(productName)) {
                    info = pi;
                    break;
                }
            }
        }

        // 3. αν ακόμα δεν βρήκαμε τίποτα -> δεν έχουμε δεδομένα γι' αυτό που ζήτησε το GUI
        if (info == null) {
            System.out.println("DEBUG: δεν βρέθηκε προϊόν για '" + productName + "'");
            return null;
        }

        // 4. Μάζεψε μόνο τις μετρήσεις μέσα στο εύρος [minYear, maxYear]
        List<MeasurementDTO> measurements = collectMeasurementsForProduct(
            info.alias,
            minYear,
            maxYear
        );

        // 5. Δώσε πίσω ProductDTO με το alias και τις μετρήσεις
        return new ProductDTO(
            info.alias,
            measurements
        );
 }*/
    
    @Override
    public ProductDTO filterProductMeasurements(String productName, int minYear, int maxYear) {

        System.out.println("== filterProductMeasurements ==");
        System.out.println("GUI asked for productName = [" + productName + "]");
        System.out.println("range = " + minYear + "-" + maxYear);

        // helper για "χαλαρή" σύγκριση
        String normRequested = normalizeName(productName);

        // 1. προσπαθώ direct alias
        ProductInfo info = productsByAlias.get(productName);

        // 2. αν δεν βρέθηκε, προσπαθώ πιο χαλαρά στα aliases
        if (info == null) {
            for (Map.Entry<String, ProductInfo> entry : productsByAlias.entrySet()) {
                String aliasKey = entry.getKey();
                if (normalizeName(aliasKey).equals(normRequested)) {
                    info = entry.getValue();
                    break;
                }
            }
        }

        // 3. αν ακόμα δεν βρήκα, προσπαθώ με το fullName
        if (info == null) {
            for (ProductInfo pi : productsByAlias.values()) {
                if (pi.fullName != null) {
                    if (normalizeName(pi.fullName).equals(normRequested)) {
                        info = pi;
                        break;
                    }
                }
            }
        }

        // 4. αν ΠΑΛΙ δεν βρέθηκε...
        if (info == null) {
            System.out.println("DEBUG: ΔΕΝ βρέθηκε προϊόν για '" + productName + "'");
            return null;
        }

        System.out.println("FOUND ProductInfo: alias=[" + info.alias + "], fullName=[" + info.fullName + "], category=[" + info.category + "]");

        // 5. μάζεψε μετρήσεις
        List<MeasurementDTO> measurements = collectMeasurementsForProduct(
            info.fullName != null ? info.fullName : info.alias,
            minYear,
            maxYear
        );

        System.out.println("measurements collected: " + measurements.size());
        for (MeasurementDTO m : measurements) {
            System.out.println("  year=" + m.getYear() + " value=" + m.getValue());
        }

        // 6. φτιάξε ProductDTO
        return new ProductDTO(
            info.alias,
            measurements
        );
    }

    /**
     * Κανονικοποιεί ονόματα για fuzzy match:
     * - lower case
     * - βγάζει κενά, παρενθέσεις, κάθετους, κόμματα κλπ
     */
    private String normalizeName(String s) {
        if (s == null) return "";
        // παράδειγμα:
        // "Natural gas avg"  -> "naturalgasavg"
        // "Natural Gas"      -> "naturalgas"
        // "Palm oil"         -> "palmoil"
        // "PalmOil"          -> "palmoil"
        return s
            .toLowerCase()
            .replaceAll("[^a-z0-9]", ""); // πέτα οτιδήποτε δεν είναι γράμμα/ψηφίο
    }

    

   


    @Override
    public List<ProductHighlightDTO> reportProductHighlights(String productAlias) {
        List<ProductHighlightDTO> out = new ArrayList<ProductHighlightDTO>();

        for (Map.Entry<Integer, YearRecord> e : yearsByYear.entrySet()) {
            Integer year = e.getKey();
            YearRecord rec = e.getValue();

            List<String> topAliases = splitTopList(rec.top10AliasesRaw);
            List<String> headlines = splitTopList(rec.top10HeadlinesRaw);
            
            System.out.println("YEAR " + year);
            System.out.println("  raw aliases = " + rec.top10AliasesRaw);
            System.out.println("  raw headlines = " + rec.top10HeadlinesRaw);
            System.out.println("  parsed aliases = " + topAliases);
            System.out.println("  looking for productAlias = " + productAlias);


            for (int i = 0; i < topAliases.size(); i++) {
                if (topAliases.get(i).equalsIgnoreCase(productAlias)) {
                	String headline = (i < headlines.size()) ? headlines.get(i) : "";
                	ProductHighlightDTO dto = new ProductHighlightDTO(year, headline);
                	out.add(dto);

                }
            }
        }

        Collections.sort(out, new Comparator<ProductHighlightDTO>() {
            @Override
            public int compare(ProductHighlightDTO a, ProductHighlightDTO b) {
                return Integer.compare(a.getYear(), b.getYear());
            }
        });
        System.out.println("DEBUG Highlights for alias = " + productAlias);
        for (ProductHighlightDTO dto : out) {
            System.out.println("  year=" + dto.getYear() + " | headline=" + dto.getHeadline());
        }

        return out;
    }

    @Override
    public List<CategoryHighlightDTO> reportCategoryHighlights(String category) {
        List<CategoryHighlightDTO> out = new ArrayList<CategoryHighlightDTO>();

        for (Map.Entry<Integer, YearRecord> e : yearsByYear.entrySet()) {
            Integer year = e.getKey();
            YearRecord rec = e.getValue();

            List<String> topAliases = splitTopList(rec.top10AliasesRaw);
            List<String> headlines = splitTopList(rec.top10HeadlinesRaw);

            for (int i = 0; i < topAliases.size(); i++) {

                String alias = topAliases.get(i);
                ProductInfo info = productsByAlias.get(alias);
                if (info == null) {
                    continue;
                }

                // ταιριάζει η κατηγορία;
                if (info.category.equalsIgnoreCase(category)) {

                    String headline = (i < headlines.size()) ? headlines.get(i) : "";
                    CategoryHighlightDTO dto = new CategoryHighlightDTO(year, alias, headline);

                    
                    out.add(dto);
                }
            }
        }

        Collections.sort(out, new Comparator<CategoryHighlightDTO>() {
            @Override
            public int compare(CategoryHighlightDTO a, CategoryHighlightDTO b) {
                return Integer.compare(a.getYear(), b.getYear());
            }
        });

        return out;
    }

    @Override
    public List<ProductStatsDTO> computeProductStats() {
        List<ProductStatsDTO> out = new ArrayList<ProductStatsDTO>();

        // για κάθε προϊόν (alias)
        for (String alias : productsByAlias.keySet()) {

            // πάρε όλες τις μετρήσεις του προϊόντος σε όλα τα έτη
            List<MeasurementDTO> ms = collectMeasurementsForProduct(
                alias,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            );

            if (ms.isEmpty()) {
                continue; // δεν έχει μετρήσεις; προχώρα στο επόμενο προϊόν
            }

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0.0;

            int latestYear = Integer.MIN_VALUE;
            double latestVal = 0.0;

            // υπολογισμοί min/max/avg/last
            for (MeasurementDTO m : ms) {
                double v = m.getValue();

                if (v < min) min = v;
                if (v > max) max = v;

                sum += v;

                if (m.getYear() > latestYear) {
                    latestYear = m.getYear();
                    latestVal = v;
                }
            }

            double avg = sum / ms.size();

            // Φτιάχνουμε το DTO σύμφωνα με τον constructor του ProductStatsDTO
            ProductStatsDTO stats = new ProductStatsDTO(
                alias,      // product (String)
                min,        // min (double)
                avg,        // average (double)
                max,        // max (double)
                latestVal   // lastValue (double)
            );

            out.add(stats);
        }

        return out;
    }


    
   /* public List<Top10AppearanceDTO> computeTop10ProductAppearances() {

        // Μετρητής: πόσες φορές κάθε κατηγορία εμφανίζεται (μέσω των προϊόντων της) στα top10
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

        for (YearRecord yr : yearsByYear.values()) {
            List<String> topAliases = splitTopList(yr.top10AliasesRaw);
            for (String alias : topAliases) {
                ProductInfo info = productsByAlias.get(alias);
                if (info == null) {
                    continue;
                }

                String category = info.category;
                Integer old = counts.get(category);
                counts.put(category, (old == null ? 1 : old + 1));
            }
        }

        // Δημιουργία των DTOs
        List<Top10AppearanceDTO> out = new ArrayList<Top10AppearanceDTO>();

        for (Map.Entry<String,Integer> e : counts.entrySet()) {
            String categoryName = e.getKey();
            Integer cnt = e.getValue();

            Top10AppearanceDTO dto = new Top10AppearanceDTO(
                categoryName,
                cnt
            );

            out.add(dto);
        }

        // ταξινόμηση φθίνουσα κατά count
        Collections.sort(out, new Comparator<Top10AppearanceDTO>() {
            @Override
            public int compare(Top10AppearanceDTO a, Top10AppearanceDTO b) {
                return Integer.compare(b.getCount(), a.getCount());
            }
        });

        return out;
    }*/
    
    public List<Top10AppearanceDTO> computeTop10ProductAppearances() {

        // Map από όνομα προϊόντος -> πόσες φορές εμφανίστηκε στα top-10
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

        for (YearRecord yr : yearsByYear.values()) {
            List<String> topAliases = splitTopList(yr.top10AliasesRaw);

            for (String alias : topAliases) {
                ProductInfo info = productsByAlias.get(alias);
                if (info == null) {
                    continue;
                }

                // Εδώ ΔΕΝ παίρνουμε την κατηγορία.
                // Παίρνουμε το πραγματικό όνομα προϊόντος.
                // Μπορείς να διαλέξεις:
                // - info.alias         (π.χ. "Oil")
                // - info.fullName      (π.χ. "Crude oil (avg)")
                // Αν θες να φαίνεται καθαρό στον πίνακα, χρησιμοποίησε fullName αν υπάρχει.

                String productName =
                    (info.fullName != null && !info.fullName.isEmpty())
                        ? info.fullName
                        : info.alias;

                Integer old = counts.get(productName);
                counts.put(productName, (old == null ? 1 : old + 1));
            }
        }

        // Δημιουργούμε DTOs
        List<Top10AppearanceDTO> out = new ArrayList<Top10AppearanceDTO>();

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String name = e.getKey();
            Integer cnt = e.getValue();

            Top10AppearanceDTO dto = new Top10AppearanceDTO(
                name,
                cnt
            );
            out.add(dto);
        }

        // Ταξινόμηση φθίνουσα κατά count (οι πιο συχνά εμφανιζόμενοι πρώτοι)
        Collections.sort(out, new Comparator<Top10AppearanceDTO>() {
            @Override
            public int compare(Top10AppearanceDTO a, Top10AppearanceDTO b) {
                return Integer.compare(b.getCount(), a.getCount());
            }
        });

        return out;
    }




    @Override
    public List<Top10AppearanceDTO> computeTop10CategoryAppearances() {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

        for (YearRecord yr : yearsByYear.values()) {
            List<String> topAliases = splitTopList(yr.top10AliasesRaw);
            for (String alias : topAliases) {
                ProductInfo info = productsByAlias.get(alias);
                if (info == null) continue;
                String cat = info.category;
                Integer old = counts.get(cat);
                counts.put(cat, (old == null ? 1 : old + 1));
            }
        }

        List<Top10AppearanceDTO> out = new ArrayList<>();

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Top10AppearanceDTO dto = new Top10AppearanceDTO(
                e.getKey(),   // name (μπορεί να είναι alias ή category)
                e.getValue()  // count
            );
            out.add(dto);
        }


        Collections.sort(out, new Comparator<Top10AppearanceDTO>() {
            @Override
            public int compare(Top10AppearanceDTO a, Top10AppearanceDTO b) {
                return Integer.compare(b.getCount(), a.getCount());
            }
        });

        return out;
    }

    @Override
    public List<YearDTO> reportAllYearsAllProductPrices() {
        List<YearDTO> out = new ArrayList<YearDTO>();
        for (Integer y : yearsByYear.keySet()) {
            YearRecord rec = yearsByYear.get(y);
            out.add(buildYearDTO(rec));
        }
        System.out.println("DEBUG: out size = " + out.size());
        return out;
    }

    // ====== HELPERS (που ήδη καλέσαμε) ======

    private void clearAll() {
        productsByAlias.clear();
        yearsByYear.clear();
        aliasColumnOrder.clear();
    }

    private void loadMetadata(String metadataPath, String delimiter) throws IOException {
        // θα συμπληρώσουμε σε επόμενο βήμα (διάβασμα metadata.tsv)
    	
    	    try (BufferedReader br = new BufferedReader(new FileReader(metadataPath))) {
    	        String line;
    	        while ((line = br.readLine()) != null) {
    	            line = line.trim();
    	            if (line.isEmpty()) continue;

    	            // κόψε τη γραμμή με βάση το delimiter (π.χ. "\t")
    	            String[] parts = line.split(delimiter);
    	            if (parts.length < 3) continue; // αν είναι σπασμένη γραμμή, την αγνοούμε

    	            ProductInfo info = new ProductInfo();
    	            info.fullName = parts[0].trim();   // π.χ. "Crude oil (average)"
    	            info.alias    = parts[1].trim();   // π.χ. "Oil"
    	            info.category = parts[2].trim();   // π.χ. "Energy"

    	            productsByAlias.put(info.alias, info);
    	        }
    	    }
    	

    }

    private int loadData(String dataPath, String delimiter) throws IOException {
        int rowsLoaded = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(dataPath))) {

            // 1. header
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Empty data file: " + dataPath);
            }

            String[] headerCols = headerLine.split(delimiter, -1);
            if (headerCols.length < 4) {
                throw new IOException("Unexpected data header format in " + dataPath);
            }

            // οι δύο τελευταίες στήλες είναι:
            //   - top10 aliases
            //   - top10 headlines
            int last = headerCols.length;
            int idxTopAliases   = last - 2;
            int idxTopHeadlines = last - 1;

            // aliasColumnOrder = οι στήλες προϊόντων (από col1 μέχρι col(idxTopAliases-1))
            aliasColumnOrder.clear();
            for (int i = 1; i < idxTopAliases; i++) {
                aliasColumnOrder.add(headerCols[i].trim());
            }

            // 2. υπόλοιπες γραμμές (μία γραμμή = ένα έτος)
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] cols = line.split(delimiter, -1);
                if (cols.length != headerCols.length) {
                    // αν η γραμμή δεν έχει ίδιο πλήθος στηλών με το header, την προσπερνάμε
                    continue;
                }

                YearRecord yr = new YearRecord();
                yr.year = parseIntSafe(cols[0]); // πρώτη στήλη = χρονιά

                // για κάθε προϊόν-στήλη (π.χ. Oil, Cocoa, Coffee...)
                for (int c = 1; c < idxTopAliases; c++) {
                    String alias = aliasColumnOrder.get(c - 1);
                    String rawVal = cols[c].trim();
                    if (!rawVal.isEmpty()) {
                        try {
                            double v = Double.parseDouble(rawVal);
                            yr.prices.put(alias, v);
                        } catch (NumberFormatException ignored) {
                            // αν δεν είναι αριθμός, το αγνοούμε
                        }
                    }
                }

                // οι τελευταίες δύο στήλες
                yr.top10AliasesRaw   = cols[idxTopAliases].trim();
                yr.top10HeadlinesRaw = cols[idxTopHeadlines].trim();

                // βάζουμε το έτος στον χάρτη
                yearsByYear.put(yr.year, yr);
                rowsLoaded++;
            }
        }

        return rowsLoaded;
    }

    

    private void loadDataWithoutMetadata(String dataPath, String delimiter) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(dataPath))) {

            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Empty data file: " + dataPath);
            }

            String[] headerCols = headerLine.split(delimiter, -1);
            if (headerCols.length < 4) {
                throw new IOException("Unexpected data header format in " + dataPath);
            }

            int last = headerCols.length;
            int idxTopAliases   = last - 2;
            int idxTopHeadlines = last - 1;

            aliasColumnOrder.clear();
            for (int i = 1; i < idxTopAliases; i++) {
                String alias = headerCols[i].trim();
                aliasColumnOrder.add(alias);

                // επειδή δεν υπάρχει metadata.tsv, δημιουργούμε ProductInfo μόνο από το alias
                if (!productsByAlias.containsKey(alias)) {
                    ProductInfo info = new ProductInfo();
                    info.alias = alias;
                    info.fullName = alias;
                    info.category = "N/A";
                    productsByAlias.put(alias, info);
                }
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] cols = line.split(delimiter, -1);
                if (cols.length != headerCols.length) {
                    continue;
                }

                YearRecord yr = new YearRecord();
                yr.year = parseIntSafe(cols[0]);

                for (int c = 1; c < idxTopAliases; c++) {
                    String alias = aliasColumnOrder.get(c - 1);
                    String rawVal = cols[c].trim();
                    if (!rawVal.isEmpty()) {
                        try {
                            double v = Double.parseDouble(rawVal);
                            yr.prices.put(alias, v);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                yr.top10AliasesRaw   = cols[idxTopAliases].trim();
                yr.top10HeadlinesRaw = cols[idxTopHeadlines].trim();

                yearsByYear.put(yr.year, yr);
            }
        }
    }


    /*private List<MeasurementDTO> collectMeasurementsForProduct(String alias, int minYear, int maxYear) {
        List<MeasurementDTO> list = new ArrayList<MeasurementDTO>();
        for (Map.Entry<Integer, YearRecord> e : yearsByYear.entrySet()) {
            int year = e.getKey();
            if (year < minYear || year > maxYear) continue;
            YearRecord yr = e.getValue();
            Double val = yr.prices.get(alias);
            if (val != null) {
                list.add(new MeasurementDTO(year, alias, val));
            }
        }
        Collections.sort(list, new Comparator<MeasurementDTO>() {
            @Override
            public int compare(MeasurementDTO a, MeasurementDTO b) {
                return Integer.compare(a.getYear(), b.getYear());
            }
        });
        return list;
    }*/
    private List<MeasurementDTO> collectMeasurementsForProduct(String alias, int minYear, int maxYear) {
        List<MeasurementDTO> list = new ArrayList<>();

        String normAlias = normalizeName(alias);

        for (Map.Entry<Integer, YearRecord> e : yearsByYear.entrySet()) {
            int year = e.getKey();
            if (year < minYear || year > maxYear) continue;

            YearRecord yr = e.getValue();
            Double val = null;

            // 1️⃣ Δοκίμασε κανονικά
            val = yr.prices.get(alias);

            // 2️⃣ Αν δεν υπάρχει, δοκίμασε να βρεις με fuzzy match στα keys
            if (val == null) {
                for (String key : yr.prices.keySet()) {
                    if (normalizeName(key).equals(normAlias)) {
                        val = yr.prices.get(key);
                        break;
                    }
                }
            }

            if (val != null) {
                list.add(new MeasurementDTO(year, alias, val));
            }
        }

        // ταξινόμηση κατά έτος
        Collections.sort(list, Comparator.comparingInt(MeasurementDTO::getYear));

        return list;
    }

    

    private YearDTO buildYearDTO(YearRecord rec) {
        // Δημιουργούμε τη λίστα με τις μετρήσεις (year, alias, value)
        List<MeasurementDTO> ms = new ArrayList<>();
        for (Map.Entry<String, Double> pr : rec.prices.entrySet()) {
            ms.add(new MeasurementDTO(rec.year, pr.getKey(), pr.getValue()));
        }

        // Διαχωρίζουμε τις λίστες aliases και headlines
        List<String> aliases = splitTopList(rec.top10AliasesRaw);
        List<String> headlines = splitTopList(rec.top10HeadlinesRaw);

        // Δημιουργούμε ένα ολοκληρωμένο YearDTO με όλα τα δεδομένα
        return new YearDTO(
            rec.year,
            ms,
            aliases,
            headlines
        );
    }
    

    private List<String> splitTopList(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return Collections.emptyList();
        }

        // Κανονικοποίηση τυχόν περίεργων whitespace (non-breaking spaces κλπ)
        raw = raw.replace('\u00A0', ' ')
                 .replace('\u2007', ' ')
                 .replace('\u202F', ' ');

        // ΣΠΑΣΙΜΟ:
        // - Χώρισε σε , ή ; ή |
        // - Φάε και κενά γύρω τους
        // ΠΡΟΣΟΧΗ: εδώ ΘΕΛΟΥΜΕ ΔΥΟ backslashes πριν το s, ΟΧΙ τέσσερα.
        String[] parts = raw.split("\\s*[;,|]\\s*");

        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }

        return out;
    }



    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
