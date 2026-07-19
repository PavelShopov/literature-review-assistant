package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.ArrayList;
import java.util.List;

public class TaxonomyImportSummary {
    private int dimensionsCreated;
    private int dimensionsUpdated;
    private int dimensionsSkipped;
    private int valuesCreated;
    private int valuesUpdated;
    private int valuesSkipped;
    private List<String> warnings = new ArrayList<>();
    private List<TaxonomyDimensionDto> dimensions = new ArrayList<>();

    public int getDimensionsCreated() {
        return dimensionsCreated;
    }

    public void incrementDimensionsCreated() {
        this.dimensionsCreated++;
    }

    public int getDimensionsUpdated() {
        return dimensionsUpdated;
    }

    public void incrementDimensionsUpdated() {
        this.dimensionsUpdated++;
    }

    public int getDimensionsSkipped() {
        return dimensionsSkipped;
    }

    public void incrementDimensionsSkipped() {
        this.dimensionsSkipped++;
    }

    public int getValuesCreated() {
        return valuesCreated;
    }

    public void incrementValuesCreated() {
        this.valuesCreated++;
    }

    public int getValuesUpdated() {
        return valuesUpdated;
    }

    public void incrementValuesUpdated() {
        this.valuesUpdated++;
    }

    public int getValuesSkipped() {
        return valuesSkipped;
    }

    public void incrementValuesSkipped() {
        this.valuesSkipped++;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<TaxonomyDimensionDto> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<TaxonomyDimensionDto> dimensions) {
        this.dimensions = dimensions;
    }
}
