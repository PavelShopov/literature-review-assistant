package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.List;

public record TaxonomyDimensionDto(
        String name,
        String description,
        List<String> options
) {
}
