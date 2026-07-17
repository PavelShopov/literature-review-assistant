package mk.ukim.finki.literaturereviewassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyDimension;
import mk.ukim.finki.literaturereviewassistant.model.TaxonomyValue;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyDimensionRepository;
import mk.ukim.finki.literaturereviewassistant.repository.TaxonomyValueRepository;
import mk.ukim.finki.literaturereviewassistant.service.TaxonomyService;
import mk.ukim.finki.literaturereviewassistant.service.TaxonomyTextNormalizer;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyDimensionDto;
import mk.ukim.finki.literaturereviewassistant.web.dto.TaxonomyImportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TaxonomyServiceImpl implements TaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyServiceImpl.class);

    private final TaxonomyDimensionRepository dimensionRepository;
    private final TaxonomyValueRepository valueRepository;
    private final ObjectMapper objectMapper;

    public TaxonomyServiceImpl(
            TaxonomyDimensionRepository dimensionRepository,
            TaxonomyValueRepository valueRepository) {
        this.dimensionRepository = dimensionRepository;
        this.valueRepository = valueRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public TaxonomyImportSummary importTaxonomy(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxonomy JSON file is required");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxonomy JSON file is empty");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read taxonomy JSON file", e);
        }

        if (bytes.length == 0 || new String(bytes, StandardCharsets.UTF_8).trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxonomy JSON file is empty");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(bytes);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded taxonomy file is not valid JSON", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse taxonomy JSON file", e);
        }

        JsonNode taxonomy = requiredObject(root, "unified_taxonomy_of_dimensions", "$");
        String taxonomyDescription = optionalText(taxonomy, "description");
        String generationDate = optionalText(taxonomy, "generation_date");
        Integer sourcePapersCount = optionalInteger(taxonomy, "source_papers_count");
        List<String> sourcePapers = stringArray(taxonomy, "source_papers", "$.unified_taxonomy_of_dimensions.source_papers", false);
        JsonNode dimensions = requiredArray(taxonomy, "dimensions", "$.unified_taxonomy_of_dimensions");

        log.info(
                "Importing taxonomy file '{}' ({} bytes, {} dimensions)",
                safeFileName(file.getOriginalFilename()),
                bytes.length,
                dimensions.size()
        );

        TaxonomyImportSummary summary = new TaxonomyImportSummary();
        List<TaxonomyDimensionDto> importedDimensions = new ArrayList<>();
        Set<String> seenDimensionsInFile = new LinkedHashSet<>();

        int dimensionIndex = 0;
        for (JsonNode dimensionNode : dimensions) {
            String path = "$.unified_taxonomy_of_dimensions.dimensions[" + dimensionIndex + "]";
            dimensionIndex++;
            if (!dimensionNode.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + " must be an object");
            }

            String category = requiredText(dimensionNode, "category", path);
            String description = requiredText(dimensionNode, "description", path);
            JsonNode possibleValues = requiredArray(dimensionNode, "possible_values", path);
            if (possibleValues.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + ".possible_values must not be empty");
            }

            String normalizedDimension = normalize(category);
            if (!seenDimensionsInFile.add(normalizedDimension)) {
                summary.getWarnings().add("Duplicate dimension '" + category + "' in uploaded file was skipped.");
                summary.incrementDimensionsSkipped();
                continue;
            }

            TaxonomyDimension dimension = dimensionRepository.findByNormalizedName(normalizedDimension)
                    .orElseGet(() -> newDimension(category, normalizedDimension));

            boolean newDimension = dimension.getTaxonomyDimensionId() == null;
            boolean dimensionChanged = applyDimensionMetadata(
                    dimension,
                    category,
                    description,
                    taxonomyDescription,
                    generationDate,
                    sourcePapersCount,
                    toJson(sourcePapers)
            );

            dimension = dimensionRepository.save(dimension);
            if (newDimension) {
                summary.incrementDimensionsCreated();
            } else if (dimensionChanged) {
                summary.incrementDimensionsUpdated();
            } else {
                summary.incrementDimensionsSkipped();
            }

            List<String> importedOptions = importValues(possibleValues, dimension, path, summary);
            importedDimensions.add(new TaxonomyDimensionDto(category, description, importedOptions));
        }

        summary.setDimensions(importedDimensions);
        log.info(
                "Taxonomy import completed: dimensions created={}, updated={}, skipped={}; values created={}, updated={}, skipped={}; warnings={}",
                summary.getDimensionsCreated(),
                summary.getDimensionsUpdated(),
                summary.getDimensionsSkipped(),
                summary.getValuesCreated(),
                summary.getValuesUpdated(),
                summary.getValuesSkipped(),
                summary.getWarnings().size()
        );
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxonomyDimensionDto> findAllDimensions() {
        return dimensionRepository.findAllByOrderByNameAsc().stream()
                .map(dimension -> new TaxonomyDimensionDto(
                        dimension.getName(),
                        dimension.getDescription(),
                        dimension.getValues().stream().map(TaxonomyValue::getValue).toList()
                ))
                .toList();
    }

    private List<String> importValues(
            JsonNode possibleValues,
            TaxonomyDimension dimension,
            String dimensionPath,
            TaxonomyImportSummary summary) {
        Map<String, String> valuesForUi = new LinkedHashMap<>();
        Set<String> seenValuesInDimension = new LinkedHashSet<>();

        int valueIndex = 0;
        for (JsonNode valueNode : possibleValues) {
            String path = dimensionPath + ".possible_values[" + valueIndex + "]";
            valueIndex++;
            if (!valueNode.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + " must be an object");
            }

            String valueText = requiredText(valueNode, "value", path);
            List<String> definedBy = stringArray(valueNode, "defined_by", path + ".defined_by", true);
            String normalizedValue = normalize(valueText);

            if (!seenValuesInDimension.add(normalizedValue)) {
                summary.getWarnings().add("Duplicate value '" + valueText + "' in dimension '" + dimension.getName() + "' was skipped.");
                summary.incrementValuesSkipped();
                continue;
            }

            TaxonomyValue value = valueRepository.findByDimensionAndNormalizedValue(dimension, normalizedValue)
                    .orElseGet(() -> newValue(dimension, valueText, normalizedValue));
            boolean newValue = value.getTaxonomyValueId() == null;
            String definedByJson = toJson(definedBy);
            boolean changed = applyValueMetadata(value, valueText, definedByJson);
            valueRepository.save(value);

            if (newValue) {
                summary.incrementValuesCreated();
            } else if (changed) {
                summary.incrementValuesUpdated();
            } else {
                summary.incrementValuesSkipped();
            }
            valuesForUi.put(normalizedValue, valueText);
        }

        return new ArrayList<>(valuesForUi.values());
    }

    private TaxonomyDimension newDimension(String name, String normalizedName) {
        TaxonomyDimension dimension = new TaxonomyDimension();
        dimension.setName(name);
        dimension.setNormalizedName(normalizedName);
        return dimension;
    }

    private TaxonomyValue newValue(TaxonomyDimension dimension, String value, String normalizedValue) {
        TaxonomyValue taxonomyValue = new TaxonomyValue();
        taxonomyValue.setDimension(dimension);
        taxonomyValue.setValue(value);
        taxonomyValue.setNormalizedValue(normalizedValue);
        return taxonomyValue;
    }

    private boolean applyDimensionMetadata(
            TaxonomyDimension dimension,
            String name,
            String description,
            String taxonomyDescription,
            String generationDate,
            Integer sourcePapersCount,
            String sourcePapersJson) {
        boolean changed = false;
        if (!Objects.equals(dimension.getName(), name)) {
            dimension.setName(name);
            changed = true;
        }
        if (!Objects.equals(dimension.getDescription(), description)) {
            dimension.setDescription(description);
            changed = true;
        }
        if (!Objects.equals(dimension.getTaxonomyDescription(), taxonomyDescription)) {
            dimension.setTaxonomyDescription(taxonomyDescription);
            changed = true;
        }
        if (!Objects.equals(dimension.getGenerationDate(), generationDate)) {
            dimension.setGenerationDate(generationDate);
            changed = true;
        }
        if (!Objects.equals(dimension.getSourcePapersCount(), sourcePapersCount)) {
            dimension.setSourcePapersCount(sourcePapersCount);
            changed = true;
        }
        if (!Objects.equals(dimension.getSourcePapersJson(), sourcePapersJson)) {
            dimension.setSourcePapersJson(sourcePapersJson);
            changed = true;
        }
        return changed;
    }

    private boolean applyValueMetadata(TaxonomyValue value, String valueText, String definedByJson) {
        boolean changed = false;
        if (!Objects.equals(value.getValue(), valueText)) {
            value.setValue(valueText);
            changed = true;
        }
        if (!Objects.equals(value.getDefinedByJson(), definedByJson)) {
            value.setDefinedByJson(definedByJson);
            changed = true;
        }
        return changed;
    }

    private JsonNode requiredObject(JsonNode parent, String field, String path) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || !node.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + "." + field + " object is required");
        }
        return node;
    }

    private JsonNode requiredArray(JsonNode parent, String field, String path) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || !node.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + "." + field + " array is required");
        }
        return node;
    }

    private String requiredText(JsonNode parent, String field, String path) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || !node.isTextual() || node.asText().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + "." + field + " is required and must be a non-blank string");
        }
        return node.asText().trim();
    }

    private String optionalText(JsonNode parent, String field) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "$.unified_taxonomy_of_dimensions." + field + " must be a string");
        }
        String value = node.asText().trim();
        return value.isBlank() ? null : value;
    }

    private Integer optionalInteger(JsonNode parent, String field) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.canConvertToInt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "$.unified_taxonomy_of_dimensions." + field + " must be an integer");
        }
        return node.asInt();
    }

    private List<String> stringArray(JsonNode parent, String field, String path, boolean required) {
        JsonNode node = parent == null ? null : parent.get(field);
        if (node == null || node.isNull()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + " array is required");
            }
            return List.of();
        }
        if (!node.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + " must be an array");
        }

        List<String> values = new ArrayList<>();
        int index = 0;
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, path + "[" + index + "] must be a non-blank string");
            }
            values.add(item.asText().trim());
            index++;
        }
        return values;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize taxonomy metadata", e);
        }
    }

    private String normalize(String value) {
        return TaxonomyTextNormalizer.normalize(value);
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded taxonomy";
        }
        return originalFilename.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
