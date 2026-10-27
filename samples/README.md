# Sample pages

Use these for manual testing, CLI runs and, later, KUMA test contexts. Turn the expected results into automated tests as your rules are implemented.

## broken-page.html

Expected issues, one row per issue:

| Rule | Element | Notes |
|---|---|---|
| image-alt | Bicycle drawing | Renders, so a screenshot exists for alt-text suggestions |
| image-alt | `missing-photo.jpg` | Broken image: no screenshot, so the user writes the alt text (F07) |
| form-label | Email field | Placeholder text is not a label |
| button-name | Icon-only button | The icon is hidden from assistive technology and there is no other name |
| link-name | Cart link | No text or other name |
| page-lang | `html` element | No `lang` attribute |
| page-title | Page | No `title` element |

Not issues: the decorative image with `alt=""`, the hidden image, the labelled Name field, the submit input, and the Help link.

## clean-page.html

The same page with every issue above fixed. The rules should report no issues. That doesn't make the page fully accessible: automated rules find only some barriers.
