# Reality check (hackathon honesty)

## 1. Real-world eligibility factors that are missing

- **Domicile / residence duration**, tehsil/district, rural vs urban flags  
- **Land ownership** quantity, **BPL / AAY / ration** category sync with schemes  
- **Caste / EWS** where schemes are category-specific (we only scratch surface in legacy rules)  
- **Age bands** (min/max) stored as structured fields — not in CSV-derived criteria  
- **Family size**, **dependency**, **disability %**, **bank / Aadhaar / mobile** linkage  
- **Scheme-specific exclusions** (already beneficiary of X), **quotas**, **waiting lists**  
- **Temporal rules** (open windows, budget exhaustion) and **versioning** of guidelines  

## 2. Data limitations that can break the system

- **Narrative eligibility → heuristic parsing** misses or misreads most schemes; many rows get **no** `eligibility_criteria` row → they never appear in `/recommend`.  
- **Composite `schemeCategory` strings** create a noisy category dimension (not a controlled vocabulary).  
- **No authoritative ministry / scheme code / API URL** in CSV — apply links often absent.  
- **Duplicate / near-duplicate schemes** across states are not deduplicated.  
- **LLM path** depends on quota, model availability, and can hallucinate explanations.  

## 3. Improving for rural users

- **Voice-first** input, **low-literacy** UI, **offline** FAQ cache  
- **Local language** coverage beyond four codes (dialects, script fallbacks)  
- **CSC / helpline** handoff with printable checklist  
- **Explain “why not eligible”** with one concrete next step (document to obtain), not generic text  
- **Smaller models or rule-only mode** when Gemini quota is zero  

## 4. What would make this hackathon-winning

- **Verified data pipeline** from an official open API or scrape with **source URL + last_updated**  
- **Human-in-the-loop** correction UI for parsed rules  
- **Demonstrable accuracy** on a labelled set of (profile → eligible schemes)  
- **District-level** personalization and **JK-specific** packaging (your original theme)  
- **Transparent scoring**: show which criteria passed/failed per scheme  

This codebase intentionally separates **demo seed + LLM** (`/match`) from **CSV + SQL rules** (`/recommend`) so you can show both “AI assist” and “auditable rules” in a pitch.
