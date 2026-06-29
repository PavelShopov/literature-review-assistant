package mk.ukim.finki.literaturereviewassistant.web.dto;

import java.util.List;

public class CriterionDto {
    private String name;
    private List<String> options;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
}
