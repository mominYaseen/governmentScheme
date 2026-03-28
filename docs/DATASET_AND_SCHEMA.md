# Dataset analysis (`updated_data.csv`) and schema

## Step 1 — Dataset understanding

### Columns (inferred types)

| Column | Inferred type | Notes |
|--------|---------------|--------|
| `scheme_name` | string | Title; required in all rows |
| `slug` | string | URL key; 3 duplicate base slugs in file (`eogu`, `vcy`) |
| `details` | string (long text) | Description |
| `benefits` | string (long text) | Benefits narrative |
| `eligibility` | string (long text) | **Primary source for structured rules** (heuristic parse) |
| `application` | string (long text) | How to apply; 2 rows empty |
| `documents` | string (long text) | List-like text; 10 rows empty |
| `level` | categorical string | Mostly `State` (2859) or `Central` (541) |
| `schemeCategory` | string | Often comma-separated multi-label (e.g. `Education & Learning, Social welfare...`) |
| *(empty 10th header)* | — | Always empty |
| `tags` | string | 29 rows empty |

**Scale:** ~3400 data rows (excluding header).

### Missing / null-like values

- `application`: 2 blank  
- `documents`: 10 blank  
- `tags`: 29 blank  
- 10th column: always blank (header artifact)

### Inconsistent formats

- **Income:** rupee symbols, lakhs, commas, prose (“below 2.5 lakh”, “family income not exceeding…”).  
- **State / geography:** full state names, “Union territory of Puducherry”, mixed English.  
- **Categories:** composite comma-separated labels (not a normalized taxonomy).  
- **Eligibility:** free text, bullets, legal phrasing — not machine-ready.

### Duplicates

- **Slug:** 3 duplicate slug values (resolved at import with `-2`, `-3` suffix on scheme `id` / `slug`).

### Fields relevant for eligibility filtering

- `eligibility` (income caps, state, gender, occupation cues)  
- `level` (central vs state programmes)  
- `schemeCategory` / `tags` (discovery, not hard rules)  
- `scheme_name` + `details` (for search / LLM; not used in rule engine)

---

## Step 2 — Schema design (normalized)

### Textual ER diagram

```
categories (id, name UNIQUE)
     ^
     | 1:N
scheme_categories (id, scheme_id FK, category_id FK, UNIQUE(scheme_id, category_id))
     |
     v
schemes (id PK, name, description, benefits, apply_process, apply_url, ministry,
         is_active, slug, gov_level, eligibility_raw, tags, source, ...)
     |
     +--- 1:N --- eligibility_criteria (id, min_income_annual, max_income_annual,
     |                                  state_codes, occupations, gender, notes)
     |
     +--- 1:N --- eligibility_rules (legacy rule engine for seed data)
     |
     +--- 1:N --- scheme_documents
```

- **`eligibility_criteria`:** typed columns (not raw text only). Raw narrative kept on `schemes.eligibility_raw`.  
- **`scheme_categories`:** resolves many-to-many between schemes and normalized category names (split on comma at import).

### DDL

Canonical DDL lives in `src/main/resources/schema.sql` (PostgreSQL).

---

## Step 8 — AI (Gemini)

Existing integration: `POST /api/schemes/match` uses Gemini for profile extraction + ranking + explanations over **legacy** `eligibility_rules` and seed schemes.  
CSV-backed **structured** flow: `POST /api/schemes/recommend` uses `eligibility_criteria` only (no Gemini required).

OpenAPI / Swagger for frontend: see [SWAGGER.md](SWAGGER.md).
