import sys
import json
import urllib.request
import urllib.parse
from urllib.error import URLError, HTTPError
import re

class Article:
    def __init__(self, title='', doi=''):
        self.title = title
        self.doi = doi

    def get_abstract(self):
        if not self.doi and not self.title:
            return ""
        
        # Try to use DOI first if available, otherwise search by title
        if self.doi:
            url = f"https://api.crossref.org/works/{urllib.parse.quote(self.doi)}"
        else:
            url = f"https://api.crossref.org/works?query.title={urllib.parse.quote(self.title)}&rows=1"

        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'LiteratureReviewAssistant/1.0 (mailto:admin@example.com)'})
            with urllib.request.urlopen(req) as response:
                data = json.loads(response.read().decode('utf-8'))
                
                item = None
                if self.doi:
                    item = data.get('message', {})
                else:
                    items = data.get('message', {}).get('items', [])
                    if items:
                        item = items[0]
                
                if item and 'abstract' in item:
                    abstract = item['abstract']
                    # Remove XML tags (e.g. <jats:p>)
                    abstract = re.sub(r'<[^>]+>', '', abstract).strip()
                    return abstract
                return ""
        except (HTTPError, URLError) as e:
            return ""

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description='Fetch abstract for an article')
    parser.add_argument('--doi', type=str, help='DOI of the article', default='')
    parser.add_argument('--title', type=str, help='Title of the article', default='')
    args = parser.parse_args()
    
    article = Article(title=args.title, doi=args.doi)
    abstract = article.get_abstract()
    if abstract:
        print(abstract)
