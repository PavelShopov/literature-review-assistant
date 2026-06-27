package mk.ukim.finki.literaturereviewassistant.service.DataService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BibTexParserTest {
    private final BibTexParser parser = new BibTexParser();

    @Test
    void parsesBibTexMetadataWithProperParser() {
        BibEntry entry = parser.parse("""
                @article{sample,
                  title={A Useful Paper},
                  author={Doe, Jane and Smith, John},
                  journal={Journal of Tests},
                  year={2025},
                  doi={https://doi.org/10.1000/Example},
                  url={https://publisher.example/paper},
                  abstract={Useful findings}
                }
                """).get(0);

        assertEquals("A Useful Paper", entry.getTitle());
        assertEquals("https://doi.org/10.1000/Example", entry.getDoi());
        assertEquals("https://publisher.example/paper", entry.getUrl());
        assertEquals(2, entry.getAuthorNames().size());
    }

    @Test
    void rejectsMalformedBibTex() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("@article{broken"));
    }
}
