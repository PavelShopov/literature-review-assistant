TEMPLATE ROUTES / CONTRACT

1. surveys-list.html
   URL: GET /survey
   Purpose: Shows all surveys in cards.

Controller should add:
model.addAttribute("surveys", surveys);

Each survey object should expose:
id
title
description
status
totalArticles
createdDate

Used links/actions:
GET  /survey/new
GET  /survey/{id}
GET  /survey/{id}/edit
POST /survey/{id}/delete


2. survey-form.html
   URL for create: GET /survey/new
   URL for edit: GET /survey/{id}/edit
   Purpose: Create/edit survey form.

For create:
POST /survey/new

For edit:
POST /survey/{id}/edit

Form field names:
title
description
status

For edit mode, controller should add:
model.addAttribute("survey", survey);

After save:
redirect:/survey


3. survey-details.html
   URL: GET /survey/{id}
   Purpose: Shows one survey and its articles/details.

Controller should add:
model.addAttribute("survey", survey);
model.addAttribute("articles", articles);

Survey fields used:
id
title
description
status
totalArticles
createdDate

Article fields likely used:
id
title
authors
journal
year
status
abstractText / articleAbstract
doi
url