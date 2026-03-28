# governmentScheme

You are building a production-quality Spring Boot application called "Government Scheme Navigator" for a hackathon. This is a backend-first system that helps Indian citizens (especially from J&K) discover government schemes they are eligible for, understand why they qualify, and get step-by-step application guidance — in their own language.

Read every instruction carefully before writing any code. Do not skip sections. Do not make assumptions where a decision is explicitly given below.

---

## TECH STACK

Backend:
- Java 21 
- Spring Boot 3.5.13
- Spring Data JPA
- PostgreSQL 16.2 (primary database)
- Maven (build tool)
- Lombok (reduce boilerplate)
- Jackson (JSON serialization)
- Spring Validation (jakarta validation annotations)

AI:
- OpenAI API (gpt-4o) via raw HTTP using Spring RestTemplate
- Used for: (1) profile extraction from free text, (2) eligibility explanation in plain language, (3) "why not eligible" nudges, (4) multilingual response generation
- The system prompt is always the first message with role "system"
- Response is always at choices[0].message.content
- Always use response_format: { type: json_object } when expecting JSON back

Frontend:
- Single HTML file served from src/main/resources/static/index.html
- Vanilla JavaScript only — no React, no Vue, no npm, no build tools
- Tailwind CSS via CDN only
- Mobile-first, works on low-end Android browsers
- Voice input via Web Speech API (browser native, no backend needed)

---

## PROJECT STRUCTURE

Create exactly this Maven project structure. Do not add extra files or rename packages.

src/
  main/
    java/com/schemenavigator/
      SchemeNavigatorApplication.java
      config/
        AppConfig.java
        CorsConfig.java
      controller/
        SchemeController.java
        HealthController.java
      service/
        ProfileExtractionService.java
        EligibilityEngineService.java
        SchemeMatchingService.java
        LlmExplanationService.java
        TranslationService.java
      model/
        Scheme.java
        EligibilityRule.java
        SchemeDocument.java
        UserProfile.java
        MatchResult.java
        MatchedScheme.java
        ApiResponse.java
      repository/
        SchemeRepository.java
        EligibilityRuleRepository.java
        SchemeDocumentRepository.java
      dto/
        UserInputRequest.java
        SchemeMatchResponse.java
      exception/
        GlobalExceptionHandler.java
        SchemeNavigatorException.java
    resources/
      application.properties
      application-dev.properties
      schema.sql
      data.sql
      static/
        index.html
  test/
    java/com/schemenavigator/
      EligibilityEngineServiceTest.java
      ProfileExtractionServiceTest.java

pom.xml
.env
.gitignore

---

## .gitignore

.env
target/
*.class
*.jar
.idea/
*.iml
.DS_Store

---

## .env (create this file, never commit it)

OPENAI_API_KEY=sk-...
DATABASE_URL=jdbc:postgresql://localhost:5432/schemenavigator
DB_USERNAME=postgres
DB_PASSWORD=postgres

---

## application.properties

spring.application.name=scheme-navigator

# Database
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/schemenavigator}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000

# JPA — NEVER use ddl-auto=create, always use schema.sql
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# SQL initialization
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
spring.sql.init.continue-on-error=true

# Server
server.port=${PORT:8080}
server.error.include-message=always
server.error.include-binding-errors=always
server.error.include-stacktrace=never

# OpenAI
openai.api.key=${OPENAI_API_KEY}
openai.api.url=https://api.openai.com/v1/chat/completions
openai.model=gpt-4o
openai.max-tokens=1000
openai.timeout-seconds=30

# Logging
logging.level.com.schemenavigator=DEBUG
logging.level.org.hibernate.SQL=WARN
logging.level.org.springframework.web=INFO

---

## application-dev.properties

spring.jpa.show-sql=true
logging.level.com.schemenavigator=DEBUG
logging.level.org.springframework.web=DEBUG

---

## schema.sql

Run this on every startup. Use IF NOT EXISTS everywhere so reruns are safe.

CREATE TABLE IF NOT EXISTS schemes (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    ministry VARCHAR(255),
    benefits TEXT,
    apply_process TEXT,
    apply_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eligibility_rules (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(50) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    value_string VARCHAR(255),
    value_number DECIMAL(15,2),
    value_boolean BOOLEAN,
    is_mandatory BOOLEAN DEFAULT true,
    failure_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scheme_documents (
    id BIGSERIAL PRIMARY KEY,
    scheme_id VARCHAR(50) NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
    document_name VARCHAR(255) NOT NULL,
    is_mandatory BOOLEAN DEFAULT true,
    description TEXT
);

Valid values for field_name column:
occupation, income_annual, age, gender, land_owned, caste_category,
bpl_card, state, is_farmer, is_student, is_disabled

Valid values for operator column:
EQUALS, NOT_EQUALS, LESS_THAN, LESS_THAN_OR_EQUAL,
GREATER_THAN, GREATER_THAN_OR_EQUAL, IN, NOT_IN, IS_TRUE, IS_FALSE

---

## data.sql

Use INSERT ... ON CONFLICT DO NOTHING everywhere so reruns are safe.

Seed exactly these 10 schemes:

-- 1. PM-KISAN
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('PM-KISAN', 'PM Kisan Samman Nidhi',
  'Income support scheme for farmer families across India',
  'Ministry of Agriculture',
  'Rs 6000 per year paid in 3 installments of Rs 2000 directly to bank account',
  'Visit nearest CSC center or apply online at pmkisan.gov.in. Carry Aadhaar and land records.',
  'https://pmkisan.gov.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('PM-KISAN', 'occupation', 'EQUALS', 'farmer', true,
  'This scheme is only for farmers. Your occupation does not match.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('PM-KISAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 200000, true,
  'Your annual income exceeds Rs 2 lakh limit for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('PM-KISAN', 'Aadhaar Card', true),
       ('PM-KISAN', 'Land Record / Khasra', true),
       ('PM-KISAN', 'Bank Passbook', true);

-- 2. PM-AWAS-RURAL
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('PM-AWAS-RURAL', 'Pradhan Mantri Awas Yojana - Gramin',
  'Housing scheme for rural poor to construct pucca houses',
  'Ministry of Rural Development',
  'Rs 1.3 lakh for hilly/J&K areas, Rs 1.2 lakh for plain areas for house construction',
  'Apply through Gram Panchayat or Block Development Officer. Registration at rhreporting.nic.in.',
  'https://rhreporting.nic.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_boolean, is_mandatory, failure_message)
VALUES ('PM-AWAS-RURAL', 'bpl_card', 'IS_TRUE', null, true,
  'This scheme requires a Below Poverty Line (BPL) card.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('PM-AWAS-RURAL', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual income must be below Rs 3 lakh for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('PM-AWAS-RURAL', 'Aadhaar Card', true),
       ('PM-AWAS-RURAL', 'BPL Card', true),
       ('PM-AWAS-RURAL', 'Bank Account Details', true),
       ('PM-AWAS-RURAL', 'Domicile Certificate', false);

-- 3. JKEDI-LOAN
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('JKEDI-LOAN', 'J&K Entrepreneurship Development Institute Loan',
  'Subsidized loans for new entrepreneurs in Jammu and Kashmir',
  'J&K Government - Industries Department',
  'Subsidized loans up to Rs 10 lakh at reduced interest rates for new businesses in J&K',
  'Apply at nearest JKEDI office or online at jkedi.org. Submit business plan and income proof.',
  'https://jkedi.org', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('JKEDI-LOAN', 'state', 'EQUALS', 'JK', true,
  'This scheme is exclusively for residents of Jammu and Kashmir.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('JKEDI-LOAN', 'age', 'GREATER_THAN_OR_EQUAL', 18, true,
  'Minimum age requirement is 18 years for this scheme.'),
       ('JKEDI-LOAN', 'age', 'LESS_THAN_OR_EQUAL', 45, true,
  'Maximum age limit is 45 years for this scheme.'),
       ('JKEDI-LOAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 500000, true,
  'Annual income must be below Rs 5 lakh for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('JKEDI-LOAN', 'Aadhaar Card', true),
       ('JKEDI-LOAN', 'Domicile Certificate of J&K', true),
       ('JKEDI-LOAN', 'Business Plan', true),
       ('JKEDI-LOAN', 'Bank Statement (6 months)', true),
       ('JKEDI-LOAN', 'Income Certificate', false);

-- 4. NSFDC-LOAN
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('NSFDC-LOAN', 'National SC Finance and Development Corporation Loan',
  'Concessional loans for Scheduled Caste individuals for livelihood activities',
  'Ministry of Social Justice and Empowerment',
  'Concessional loans at 6% per annum interest rate for income generating activities',
  'Apply through State Channelising Agencies or online at nsfdc.nic.in',
  'https://nsfdc.nic.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('NSFDC-LOAN', 'caste_category', 'EQUALS', 'SC', true,
  'This scheme is only for Scheduled Caste individuals. Caste certificate required.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('NSFDC-LOAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual family income must be below Rs 3 lakh for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('NSFDC-LOAN', 'Aadhaar Card', true),
       ('NSFDC-LOAN', 'Caste Certificate (SC)', true),
       ('NSFDC-LOAN', 'Income Certificate', true),
       ('NSFDC-LOAN', 'Bank Passbook', true);

-- 5. PM-SVANidhi
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('PM-SVANidhi', 'PM Street Vendor Atmanirbhar Nidhi',
  'Collateral-free working capital loans for street vendors',
  'Ministry of Housing and Urban Affairs',
  'Collateral-free loan of Rs 10,000 initially, extendable to Rs 20,000 and Rs 50,000',
  'Apply at pmsvanidhi.mohua.gov.in or through nearby Urban Local Body office',
  'https://pmsvanidhi.mohua.gov.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('PM-SVANidhi', 'occupation', 'IN', 'street_vendor,vendor,hawker', true,
  'This scheme is only for street vendors and hawkers.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('PM-SVANidhi', 'Aadhaar Card', true),
       ('PM-SVANidhi', 'Vendor Certificate or ULB Letter', true),
       ('PM-SVANidhi', 'Bank Account Details', true);

-- 6. MUDRA-SHISHU
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('MUDRA-SHISHU', 'Pradhan Mantri MUDRA Yojana - Shishu',
  'Micro loans for small non-farm businesses',
  'Ministry of Finance',
  'Loan up to Rs 50,000 for micro enterprises at competitive interest rates',
  'Apply at any nationalized bank, regional rural bank, or microfinance institution',
  'https://mudra.org.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('MUDRA-SHISHU', 'income_annual', 'LESS_THAN_OR_EQUAL', 500000, true,
  'Annual income should be below Rs 5 lakh for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('MUDRA-SHISHU', 'Aadhaar Card', true),
       ('MUDRA-SHISHU', 'Business Proof or Plan', true),
       ('MUDRA-SHISHU', 'Bank Statement (3 months)', true),
       ('MUDRA-SHISHU', 'Passport Size Photo', true);

-- 7. NSP-SCHOLARSHIP
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('NSP-SCHOLARSHIP', 'National Scholarship Portal - Post Matric Scholarship',
  'Scholarship for SC, ST, OBC students pursuing post-matriculation education',
  'Ministry of Social Justice and Empowerment',
  'Tuition fee reimbursement plus maintenance allowance for hostel and day scholars',
  'Apply online at scholarships.gov.in before the annual deadline (usually October)',
  'https://scholarships.gov.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_boolean, is_mandatory, failure_message)
VALUES ('NSP-SCHOLARSHIP', 'is_student', 'IS_TRUE', null, true,
  'This scholarship is only for currently enrolled students.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('NSP-SCHOLARSHIP', 'income_annual', 'LESS_THAN_OR_EQUAL', 250000, true,
  'Family annual income must be below Rs 2.5 lakh for this scholarship.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('NSP-SCHOLARSHIP', 'caste_category', 'IN', 'SC,ST,OBC', true,
  'This scholarship is for SC, ST, and OBC category students only.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('NSP-SCHOLARSHIP', 'Aadhaar Card', true),
       ('NSP-SCHOLARSHIP', 'Income Certificate', true),
       ('NSP-SCHOLARSHIP', 'Caste Certificate', true),
       ('NSP-SCHOLARSHIP', 'Previous Year Marksheet', true),
       ('NSP-SCHOLARSHIP', 'College / Institution ID Card', true),
       ('NSP-SCHOLARSHIP', 'Bank Passbook', true);

-- 8. JKPMC-MUMKIN
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('JKPMC-MUMKIN', 'J&K Mumkin Scheme',
  'Skill development and employment scheme for J&K youth',
  'J&K Government - Planning, Development and Monitoring Department',
  'Free skill training plus Rs 10,000 per month stipend during training period',
  'Register at jkpmc.gov.in or visit nearest District Employment Center',
  'https://jkpmc.gov.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('JKPMC-MUMKIN', 'state', 'EQUALS', 'JK', true,
  'This scheme is exclusively for residents of Jammu and Kashmir.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('JKPMC-MUMKIN', 'age', 'GREATER_THAN_OR_EQUAL', 18, true,
  'Minimum age is 18 years for this scheme.'),
       ('JKPMC-MUMKIN', 'age', 'LESS_THAN_OR_EQUAL', 35, true,
  'Maximum age limit is 35 years for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('JKPMC-MUMKIN', 'Aadhaar Card', true),
       ('JKPMC-MUMKIN', 'Domicile Certificate of J&K', true),
       ('JKPMC-MUMKIN', 'Qualification Certificate', true),
       ('JKPMC-MUMKIN', 'Bank Account Details', true);

-- 9. PM-UJJWALA
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('PM-UJJWALA', 'Pradhan Mantri Ujjwala Yojana',
  'Free LPG connections for women from BPL households',
  'Ministry of Petroleum and Natural Gas',
  'Free LPG connection plus first refill cylinder free of cost',
  'Visit nearest LPG distributor with documents or apply at pmuy.gov.in',
  'https://pmuy.gov.in', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
VALUES ('PM-UJJWALA', 'gender', 'EQUALS', 'female', true,
  'This scheme is only for women beneficiaries.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_boolean, is_mandatory, failure_message)
VALUES ('PM-UJJWALA', 'bpl_card', 'IS_TRUE', null, true,
  'A BPL (Below Poverty Line) card is required for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('PM-UJJWALA', 'Aadhaar Card', true),
       ('PM-UJJWALA', 'BPL Card', true),
       ('PM-UJJWALA', 'Bank Account Details', true),
       ('PM-UJJWALA', 'Passport Size Photo', true);

-- 10. KISAN-CREDIT-CARD
INSERT INTO schemes (id, name, description, ministry, benefits, apply_process, apply_url, is_active)
VALUES ('KISAN-CREDIT-CARD', 'Kisan Credit Card',
  'Revolving credit facility for farmers for agricultural and allied activities',
  'Ministry of Agriculture and Farmers Welfare',
  'Revolving credit up to Rs 3 lakh at 4% effective interest rate per annum',
  'Apply at any nationalized bank, cooperative bank, or regional rural bank with land records',
  'https://pmkisan.gov.in/KCC.aspx', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_boolean, is_mandatory, failure_message)
VALUES ('KISAN-CREDIT-CARD', 'is_farmer', 'IS_TRUE', null, true,
  'Kisan Credit Card is only available for farmers and cultivators.');

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
VALUES ('KISAN-CREDIT-CARD', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual income should be below Rs 3 lakh for this scheme.');

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
VALUES ('KISAN-CREDIT-CARD', 'Aadhaar Card', true),
       ('KISAN-CREDIT-CARD', 'Land Record / Khasra', true),
       ('KISAN-CREDIT-CARD', 'Passport Size Photo', true),
       ('KISAN-CREDIT-CARD', 'Bank Account Details', true);

---

## JAVA MODEL CLASSES

### SchemeNavigatorApplication.java

@SpringBootApplication
public class SchemeNavigatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemeNavigatorApplication.class, args);
    }
}

### UserProfile.java

Not a JPA entity. Plain Java class used only during request processing.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String occupation;
    private Long incomeAnnual;
    private String location;
    private String state;
    private String gender;
    private Boolean landOwned;
    private String casteCategory;
    private Integer age;
    private Boolean bplCard;
    private Boolean isFarmer;
    private Boolean isStudent;
    private Boolean isDisabled;
    private String rawInput;
    private String detectedLanguage;
}

### Scheme.java

@Entity
@Table(name = "schemes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scheme {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ministry;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "apply_process", columnDefinition = "TEXT")
    private String applyProcess;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EligibilityRule> eligibilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SchemeDocument> documents = new ArrayList<>();
}

### EligibilityRule.java

@Entity
@Table(name = "eligibility_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String operator;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "value_number")
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;
}

### SchemeDocument.java

@Entity
@Table(name = "scheme_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    private String description;
}

### MatchResult.java

Not a JPA entity.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    private boolean eligible;
    private List<String> passedRules;
    private List<String> failedRules;
    private List<String> skippedRules;
    private double eligibilityScore;
}

### MatchedScheme.java

Not a JPA entity.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedScheme {
    private Scheme scheme;
    private MatchResult matchResult;
}

### ApiResponse.java

Generic wrapper for all API responses.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(message)
            .timestamp(Instant.now().toString())
            .build();
    }
}

---

## DTOs

### UserInputRequest.java

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInputRequest {
    @NotBlank(message = "User input cannot be empty")
    @Size(min = 3, max = 2000, message = "Input must be between 3 and 2000 characters")
    private String userInput;

    private String language;
}

### SchemeMatchResponse.java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeMatchResponse {
    private UserProfile userProfile;
    private List<EligibleSchemeDto> eligibleSchemes;
    private List<NearMissSchemeDto> nearMissSchemes;
    private String summaryMessage;
    private String detectedLanguage;
    private long processingTimeMs;
    private int totalSchemesChecked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibleSchemeDto {
        private String schemeId;
        private String schemeName;
        private String ministry;
        private String benefits;
        private String applyUrl;
        private String whyEligible;
        private String howToApply;
        private List<String> documentsNeeded;
        private List<String> passedRules;
        private double eligibilityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearMissSchemeDto {
        private String schemeId;
        private String schemeName;
        private String benefits;
        private String whyNotEligible;
        private String whatToDo;
        private double eligibilityScore;
    }
}

---

## REPOSITORIES

### SchemeRepository.java

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, String> {

    @Query("SELECT DISTINCT s FROM Scheme s " +
           "LEFT JOIN FETCH s.eligibilityRules " +
           "LEFT JOIN FETCH s.documents " +
           "WHERE s.isActive = true")
    List<Scheme> findAllActiveWithRules();

    List<Scheme> findByIsActiveTrue();
}

This JOIN FETCH query is critical. It prevents N+1 queries.
Always use findAllActiveWithRules() in SchemeMatchingService, never findAll().

### EligibilityRuleRepository.java

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, Long> {
    List<EligibilityRule> findBySchemeId(String schemeId);
}

### SchemeDocumentRepository.java

@Repository
public interface SchemeDocumentRepository extends JpaRepository<SchemeDocument, Long> {
    List<SchemeDocument> findBySchemeId(String schemeId);
}

---

## CONFIG CLASSES

### AppConfig.java

@Configuration
public class AppConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Value("${openai.model}")
    private String openAiModel;

    @Value("${openai.max-tokens:1000}")
    private int maxTokens;

    @Value("${openai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public String getOpenAiApiKey() { return openAiApiKey; }
    public String getOpenAiApiUrl() { return openAiApiUrl; }
    public String getOpenAiModel() { return openAiModel; }
    public int getMaxTokens() { return maxTokens; }
}

### CorsConfig.java

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }
}

---

## SERVICE IMPLEMENTATIONS

### ProfileExtractionService.java

This service converts raw user text into a structured UserProfile using OpenAI.

Declare this as a constant inside the class:

private static final String SYSTEM_PROMPT = """
    You are a profile extraction assistant for an Indian government scheme eligibility checker.
    Extract structured information from the user message and return ONLY valid JSON.
    No explanation, no markdown, no code blocks. Just the raw JSON object.

    Return exactly this JSON structure:
    {
      "occupation": "string or null — use one of: farmer, student, vendor, labourer, shopkeeper, government_employee, unemployed, other",
      "income_annual": "number in rupees or null — convert lakhs to rupees, 1 lakh = 100000",
      "location": "string or null — city or district name as mentioned",
      "state": "2-letter state code or null — use JK for any J&K location like Srinagar, Jammu, Baramulla, Sopore, Anantnag, Kupwara, Pulwama, Kargil, Leh, Kathua, Udhampur",
      "gender": "male or female or other or null",
      "land_owned": true or false or null,
      "caste_category": "GEN or OBC or SC or ST or null",
      "age": number or null,
      "bpl_card": true or false or null,
      "is_student": true or false or null — true if occupation is student or mentions college or school,
      "is_farmer": true or false or null — true if occupation is farmer,
      "is_disabled": true or false or null,
      "detected_language": "en or hi or ur or ks"
    }

    Rules:
    - Return null for any field not clearly mentioned. Do not guess or assume.
    - income_annual: if user says "1 lakh" return 100000, "1.5 lakh" return 150000, "80 thousand" return 80000
    - state: if any J&K district or city is mentioned, always set state to JK
    - detected_language: en for English, hi for Hindi/Devanagari, ur for Urdu/Nastaliq, ks for Kashmiri
    - is_farmer must be true whenever occupation is farmer
    - is_student must be true whenever occupation is student or user mentions studying
    """;

Implement this method:

public UserProfile extractProfile(String userInput) {
    try {
        // Step 1: Sanitize input
        String sanitized = userInput.trim();
        if (sanitized.length() > 2000) {
            sanitized = sanitized.substring(0, 2000);
        }

        // Step 2: Build OpenAI request
        // messages array: first the system message, then the user message
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", sanitized));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", appConfig.getOpenAiModel());
        requestBody.put("max_tokens", 500);
        requestBody.put("messages", messages);
        requestBody.put("response_format", Map.of("type", "json_object"));

        // Step 3: Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + appConfig.getOpenAiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Step 4: Call OpenAI
        ResponseEntity<Map> response = restTemplate.exchange(
            appConfig.getOpenAiApiUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Step 5: Extract text from response
        // OpenAI response path: choices[0].message.content
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String jsonText = (String) message.get("content");

        // Step 6: Parse JSON into UserProfile
        return parseProfileFromJson(jsonText, userInput);

    } catch (Exception e) {
        log.error("OpenAI profile extraction failed: {}. Falling back to keyword extraction.", e.getMessage());
        return keywordFallbackExtraction(userInput);
    }
}

private UserProfile parseProfileFromJson(String jsonText, String rawInput) {
    try {
        Map<String, Object> data = objectMapper.readValue(jsonText, Map.class);

        UserProfile.UserProfileBuilder builder = UserProfile.builder();
        builder.rawInput(rawInput);

        if (data.get("occupation") != null)
            builder.occupation(data.get("occupation").toString().toLowerCase());

        if (data.get("income_annual") != null) {
            try {
                builder.incomeAnnual(Long.parseLong(data.get("income_annual").toString().replace(".0", "")));
            } catch (NumberFormatException ex) {
                log.warn("Could not parse income_annual: {}", data.get("income_annual"));
            }
        }

        if (data.get("location") != null)
            builder.location(data.get("location").toString());

        if (data.get("state") != null)
            builder.state(data.get("state").toString().toUpperCase());

        if (data.get("gender") != null)
            builder.gender(data.get("gender").toString().toLowerCase());

        if (data.get("land_owned") != null)
            builder.landOwned(Boolean.parseBoolean(data.get("land_owned").toString()));

        if (data.get("caste_category") != null)
            builder.casteCategory(data.get("caste_category").toString().toUpperCase());

        if (data.get("age") != null) {
            try {
                builder.age(Integer.parseInt(data.get("age").toString().replace(".0", "")));
            } catch (NumberFormatException ex) {
                log.warn("Could not parse age: {}", data.get("age"));
            }
        }

        if (data.get("bpl_card") != null)
            builder.bplCard(Boolean.parseBoolean(data.get("bpl_card").toString()));

        if (data.get("is_farmer") != null)
            builder.isFarmer(Boolean.parseBoolean(data.get("is_farmer").toString()));

        if (data.get("is_student") != null)
            builder.isStudent(Boolean.parseBoolean(data.get("is_student").toString()));

        if (data.get("is_disabled") != null)
            builder.isDisabled(Boolean.parseBoolean(data.get("is_disabled").toString()));

        if (data.get("detected_language") != null)
            builder.detectedLanguage(data.get("detected_language").toString());
        else
            builder.detectedLanguage("en");

        UserProfile profile = builder.build();

        // Derive isFarmer from occupation if not already set
        if (profile.getIsFarmer() == null && "farmer".equals(profile.getOccupation())) {
            profile.setIsFarmer(true);
        }
        // Derive isStudent from occupation if not already set
        if (profile.getIsStudent() == null && "student".equals(profile.getOccupation())) {
            profile.setIsStudent(true);
        }

        return profile;

    } catch (Exception e) {
        log.error("Failed to parse LLM JSON response: {}. Raw JSON: {}", e.getMessage(), jsonText);
        return keywordFallbackExtraction(rawInput);
    }
}

private UserProfile keywordFallbackExtraction(String input) {
    String lower = input.toLowerCase();
    UserProfile.UserProfileBuilder builder = UserProfile.builder();
    builder.rawInput(input);
    builder.detectedLanguage("en");

    // Occupation detection
    if (lower.contains("farmer") || lower.contains("kisan") || lower.contains("krishak") || lower.contains("kheti")) {
        builder.occupation("farmer");
        builder.isFarmer(true);
    } else if (lower.contains("student") || lower.contains("padhai") || lower.contains("college") || lower.contains("school")) {
        builder.occupation("student");
        builder.isStudent(true);
    } else if (lower.contains("vendor") || lower.contains("hawker") || lower.contains("dukaan")) {
        builder.occupation("vendor");
    } else if (lower.contains("labour") || lower.contains("mazdoor")) {
        builder.occupation("labourer");
    }

    // Income detection
    if (lower.contains("lakh") || lower.contains("lac")) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)\\s*(lakh|lac)");
        java.util.regex.Matcher m = p.matcher(lower);
        if (m.find()) {
            try {
                double lakhs = Double.parseDouble(m.group(1));
                builder.incomeAnnual((long)(lakhs * 100000));
            } catch (NumberFormatException ignored) {}
        }
    }

    // State detection from J&K districts
    String[] jkLocations = {"srinagar", "jammu", "baramulla", "sopore", "anantnag",
                             "kupwara", "pulwama", "kargil", "leh", "kathua", "udhampur",
                             "kashmir", "j&k", "jk"};
    for (String loc : jkLocations) {
        if (lower.contains(loc)) {
            builder.state("JK");
            break;
        }
    }

    // Caste detection
    if (lower.contains(" sc ") || lower.contains("scheduled caste")) builder.casteCategory("SC");
    else if (lower.contains(" st ") || lower.contains("scheduled tribe")) builder.casteCategory("ST");
    else if (lower.contains("obc") || lower.contains("other backward")) builder.casteCategory("OBC");

    // Gender detection
    if (lower.contains(" female") || lower.contains("woman") || lower.contains("wife") || lower.contains("aurat")) {
        builder.gender("female");
    } else if (lower.contains(" male") || lower.contains("man ") || lower.contains("aadmi")) {
        builder.gender("male");
    }

    // BPL detection
    if (lower.contains("bpl") || lower.contains("below poverty")) builder.bplCard(true);

    return builder.build();
}

### EligibilityEngineService.java

THIS SERVICE MUST CONTAIN ZERO HTTP CALLS AND ZERO LLM CALLS.
It is pure deterministic Java logic only.

@Service
@Slf4j
public class EligibilityEngineService {

    public MatchResult evaluate(UserProfile profile, Scheme scheme) {
        List<String> passedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
        List<String> skippedRules = new ArrayList<>();
        int mandatoryRuleCount = 0;

        List<EligibilityRule> rules = scheme.getEligibilityRules();
        if (rules == null || rules.isEmpty()) {
            return MatchResult.builder()
                .eligible(true)
                .passedRules(passedRules)
                .failedRules(failedRules)
                .skippedRules(skippedRules)
                .eligibilityScore(1.0)
                .build();
        }

        for (EligibilityRule rule : rules) {
            try {
                if (Boolean.TRUE.equals(rule.getIsMandatory())) {
                    mandatoryRuleCount++;
                }

                Object userValue = getFieldValue(profile, rule.getFieldName());

                if (userValue == null) {
                    skippedRules.add("Unknown: " + rule.getFieldName());
                    continue;
                }

                boolean passed = applyOperator(userValue, rule);

                if (passed) {
                    passedRules.add(buildPassMessage(rule, userValue));
                } else {
                    if (Boolean.TRUE.equals(rule.getIsMandatory())) {
                        failedRules.add(rule.getFailureMessage() != null
                            ? rule.getFailureMessage()
                            : "Failed rule: " + rule.getFieldName());
                    }
                }
            } catch (Exception e) {
                log.warn("Error evaluating rule {} for scheme {}: {}",
                    rule.getFieldName(), scheme.getId(), e.getMessage());
                skippedRules.add("Error evaluating: " + rule.getFieldName());
            }
        }

        boolean eligible = failedRules.isEmpty();
        double score = mandatoryRuleCount > 0
            ? (double) passedRules.size() / mandatoryRuleCount
            : 1.0;
        score = Math.min(score, 1.0);

        return MatchResult.builder()
            .eligible(eligible)
            .passedRules(passedRules)
            .failedRules(failedRules)
            .skippedRules(skippedRules)
            .eligibilityScore(score)
            .build();
    }

    private Object getFieldValue(UserProfile profile, String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "occupation"     -> profile.getOccupation();
            case "income_annual"  -> profile.getIncomeAnnual();
            case "age"            -> profile.getAge();
            case "gender"         -> profile.getGender();
            case "land_owned"     -> profile.getLandOwned();
            case "caste_category" -> profile.getCasteCategory();
            case "bpl_card"       -> profile.getBplCard();
            case "state"          -> profile.getState();
            case "is_farmer"      -> profile.getIsFarmer();
            case "is_student"     -> profile.getIsStudent();
            case "is_disabled"    -> profile.getIsDisabled();
            default               -> null;
        };
    }

    private boolean applyOperator(Object userValue, EligibilityRule rule) {
        return switch (rule.getOperator().toUpperCase()) {
            case "EQUALS" ->
                userValue.toString().equalsIgnoreCase(rule.getValueString());

            case "NOT_EQUALS" ->
                !userValue.toString().equalsIgnoreCase(rule.getValueString());

            case "LESS_THAN" ->
                toLong(userValue) < rule.getValueNumber().longValue();

            case "LESS_THAN_OR_EQUAL" ->
                toLong(userValue) <= rule.getValueNumber().longValue();

            case "GREATER_THAN" ->
                toLong(userValue) > rule.getValueNumber().longValue();

            case "GREATER_THAN_OR_EQUAL" ->
                toLong(userValue) >= rule.getValueNumber().longValue();

            case "IN" -> {
                if (rule.getValueString() == null) yield false;
                String[] values = rule.getValueString().split(",");
                yield Arrays.stream(values)
                    .anyMatch(v -> v.trim().equalsIgnoreCase(userValue.toString()));
            }

            case "NOT_IN" -> {
                if (rule.getValueString() == null) yield true;
                String[] values = rule.getValueString().split(",");
                yield Arrays.stream(values)
                    .noneMatch(v -> v.trim().equalsIgnoreCase(userValue.toString()));
            }

            case "IS_TRUE" ->
                Boolean.TRUE.equals(userValue) || "true".equalsIgnoreCase(userValue.toString());

            case "IS_FALSE" ->
                Boolean.FALSE.equals(userValue) || "false".equalsIgnoreCase(userValue.toString());

            default -> {
                log.warn("Unknown operator: {}", rule.getOperator());
                yield false;
            }
        };
    }

    private long toLong(Object value) {
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private String buildPassMessage(EligibilityRule rule, Object userValue) {
        return switch (rule.getOperator().toUpperCase()) {
            case "EQUALS"               -> rule.getFieldName() + " is " + userValue;
            case "LESS_THAN_OR_EQUAL"   -> rule.getFieldName() + " (Rs " + userValue + ") is within limit";
            case "GREATER_THAN_OR_EQUAL"-> rule.getFieldName() + " (" + userValue + ") meets minimum";
            case "IS_TRUE"              -> rule.getFieldName() + " confirmed";
            case "IN"                   -> rule.getFieldName() + " (" + userValue + ") is in eligible category";
            default                     -> rule.getFieldName() + " check passed";
        };
    }
}

### SchemeMatchingService.java

@Service
@Slf4j
public class SchemeMatchingService {

    private final SchemeRepository schemeRepository;
    private final EligibilityEngineService eligibilityEngine;

    public SchemeMatchingService(SchemeRepository schemeRepository,
                                  EligibilityEngineService eligibilityEngine) {
        this.schemeRepository = schemeRepository;
        this.eligibilityEngine = eligibilityEngine;
    }

    public List<MatchedScheme> matchAll(UserProfile profile) {
        List<Scheme> allSchemes = schemeRepository.findAllActiveWithRules();

        if (allSchemes.isEmpty()) {
            log.warn("No active schemes found in database. Check data.sql seeding.");
        }

        List<MatchedScheme> eligible = new ArrayList<>();
        List<MatchedScheme> nearMiss = new ArrayList<>();

        for (Scheme scheme : allSchemes) {
            MatchResult result = eligibilityEngine.evaluate(profile, scheme);
            MatchedScheme matchedScheme = MatchedScheme.builder()
                .scheme(scheme)
                .matchResult(result)
                .build();

            if (result.isEligible()) {
                eligible.add(matchedScheme);
            } else if (result.getEligibilityScore() >= 0.5) {
                nearMiss.add(matchedScheme);
            }
        }

        // Sort eligible by number of passed rules descending
        eligible.sort((a, b) ->
            b.getMatchResult().getPassedRules().size() - a.getMatchResult().getPassedRules().size());

        // Sort near-miss by score descending
        nearMiss.sort((a, b) ->
            Double.compare(b.getMatchResult().getEligibilityScore(),
                           a.getMatchResult().getEligibilityScore()));

        List<MatchedScheme> result = new ArrayList<>();
        result.addAll(eligible.stream().limit(5).toList());
        result.addAll(nearMiss.stream().limit(3).toList());

        log.debug("Matched {} eligible and {} near-miss schemes for profile: {}",
            eligible.size(), nearMiss.size(), profile.getOccupation());

        return result;
    }
}

### LlmExplanationService.java

Declare this system prompt constant:

private static final String SYSTEM_PROMPT = """
    You are a friendly and empathetic government scheme advisor helping Indian citizens,
    especially from rural and semi-urban areas of Jammu and Kashmir.
    
    Explain eligibility results in simple, warm, encouraging language.
    Avoid jargon. Be specific and actionable.
    Keep explanations under 150 words per scheme.
    
    Always respond in the language specified. Language codes:
    - en: English
    - hi: Hindi in Devanagari script
    - ur: Urdu in Nastaliq script
    - ks: Kashmiri (use Hindi if Kashmiri is not possible)
    
    Always keep scheme names, URLs, and document names in English even when responding in other languages.
    
    You must respond with ONLY a valid JSON object. No explanation, no markdown, no code blocks.
    """;

Implement this method:

public Map<String, Object> generateExplanation(UserProfile profile, List<MatchedScheme> matchedSchemes) {
    try {
        String userMessage = buildUserMessage(profile, matchedSchemes);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", appConfig.getOpenAiModel());
        requestBody.put("max_tokens", 1500);
        requestBody.put("messages", messages);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + appConfig.getOpenAiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            appConfig.getOpenAiApiUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        String jsonText = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

        return objectMapper.readValue(jsonText, Map.class);

    } catch (Exception e) {
        log.error("LLM explanation failed: {}. Returning fallback.", e.getMessage());
        return buildFallbackExplanation(matchedSchemes);
    }
}

private String buildUserMessage(UserProfile profile, List<MatchedScheme> matchedSchemes) {
    StringBuilder sb = new StringBuilder();
    sb.append("Language to respond in: ").append(profile.getDetectedLanguage()).append("\n\n");
    sb.append("User profile:\n");
    if (profile.getOccupation() != null) sb.append("- Occupation: ").append(profile.getOccupation()).append("\n");
    if (profile.getIncomeAnnual() != null) sb.append("- Annual income: Rs ").append(profile.getIncomeAnnual()).append("\n");
    if (profile.getLocation() != null) sb.append("- Location: ").append(profile.getLocation()).append("\n");
    if (profile.getState() != null) sb.append("- State: ").append(profile.getState()).append("\n");
    if (profile.getCasteCategory() != null) sb.append("- Caste: ").append(profile.getCasteCategory()).append("\n");
    if (profile.getAge() != null) sb.append("- Age: ").append(profile.getAge()).append("\n");
    if (profile.getGender() != null) sb.append("- Gender: ").append(profile.getGender()).append("\n");
    if (profile.getBplCard() != null) sb.append("- BPL card: ").append(profile.getBplCard()).append("\n");

    List<MatchedScheme> eligible = matchedSchemes.stream()
        .filter(m -> m.getMatchResult().isEligible()).toList();
    List<MatchedScheme> nearMiss = matchedSchemes.stream()
        .filter(m -> !m.getMatchResult().isEligible()).toList();

    sb.append("\nEligible schemes:\n");
    for (MatchedScheme ms : eligible) {
        sb.append("- ").append(ms.getScheme().getName())
          .append(" (ID: ").append(ms.getScheme().getId()).append(")\n");
        sb.append("  Benefits: ").append(ms.getScheme().getBenefits()).append("\n");
        sb.append("  Apply: ").append(ms.getScheme().getApplyProcess()).append("\n");
        sb.append("  Passed rules: ").append(ms.getMatchResult().getPassedRules()).append("\n");
        sb.append("  Documents: ");
        if (ms.getScheme().getDocuments() != null) {
            ms.getScheme().getDocuments().forEach(d -> sb.append(d.getDocumentName()).append(", "));
        }
        sb.append("\n");
    }

    sb.append("\nNear-miss schemes (almost eligible):\n");
    for (MatchedScheme ms : nearMiss) {
        sb.append("- ").append(ms.getScheme().getName())
          .append(" (ID: ").append(ms.getScheme().getId()).append(")\n");
        sb.append("  Failed rules: ").append(ms.getMatchResult().getFailedRules()).append("\n");
    }

    sb.append("""

    Respond with this exact JSON structure:
    {
      "eligible_explanations": [
        {
          "scheme_id": "...",
          "why_eligible": "...",
          "how_to_apply": "...",
          "documents_needed": ["...", "..."]
        }
      ],
      "near_miss_explanations": [
        {
          "scheme_id": "...",
          "why_not_eligible": "...",
          "what_to_do": "..."
        }
      ],
      "summary_message": "1-2 sentence encouraging summary in the detected language"
    }
    """);

    return sb.toString();
}

private Map<String, Object> buildFallbackExplanation(List<MatchedScheme> matchedSchemes) {
    List<Map<String, Object>> eligibleExplanations = new ArrayList<>();
    List<Map<String, Object>> nearMissExplanations = new ArrayList<>();

    for (MatchedScheme ms : matchedSchemes) {
        if (ms.getMatchResult().isEligible()) {
            List<String> docs = ms.getScheme().getDocuments() == null
                ? List.of()
                : ms.getScheme().getDocuments().stream()
                    .map(SchemeDocument::getDocumentName).toList();

            eligibleExplanations.add(Map.of(
                "scheme_id", ms.getScheme().getId(),
                "why_eligible", "You meet the eligibility criteria for this scheme.",
                "how_to_apply", ms.getScheme().getApplyProcess() != null
                    ? ms.getScheme().getApplyProcess() : "Visit the official website to apply.",
                "documents_needed", docs
            ));
        } else {
            nearMissExplanations.add(Map.of(
                "scheme_id", ms.getScheme().getId(),
                "why_not_eligible", String.join("; ", ms.getMatchResult().getFailedRules()),
                "what_to_do", "Review the eligibility criteria and gather required documents."
            ));
        }
    }

    return Map.of(
        "eligible_explanations", eligibleExplanations,
        "near_miss_explanations", nearMissExplanations,
        "summary_message", "Here are the schemes we found for you. Please visit the official websites to apply."
    );
}

### TranslationService.java

@Service
@Slf4j
public class TranslationService {

    public String translateToLanguage(String text, String targetLanguage) {
        // Stub for future implementation
        // LlmExplanationService already handles translation inline
        log.debug("Translation stub called for language: {}", targetLanguage);
        return text;
    }
}

---

## CONTROLLER

### SchemeController.java

@RestController
@RequestMapping("/api/schemes")
@Slf4j
public class SchemeController {

    private final ProfileExtractionService profileExtractionService;
    private final SchemeMatchingService schemeMatchingService;
    private final LlmExplanationService llmExplanationService;
    private final SchemeRepository schemeRepository;

    public SchemeController(ProfileExtractionService profileExtractionService,
                             SchemeMatchingService schemeMatchingService,
                             LlmExplanationService llmExplanationService,
                             SchemeRepository schemeRepository) {
        this.profileExtractionService = profileExtractionService;
        this.schemeMatchingService = schemeMatchingService;
        this.llmExplanationService = llmExplanationService;
        this.schemeRepository = schemeRepository;
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<SchemeMatchResponse>> matchSchemes(
            @Valid @RequestBody UserInputRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("Received match request. Input length: {}", request.getUserInput().length());

        // Step 1: Extract profile from raw text
        UserProfile profile = profileExtractionService.extractProfile(request.getUserInput());
        
        // Override detected language if explicitly provided
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            profile.setDetectedLanguage(request.getLanguage());
        }
