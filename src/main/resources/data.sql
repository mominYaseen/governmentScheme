-- Idempotent seed: INSERT ... SELECT ... WHERE NOT EXISTS (no unique indexes required on child tables).

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
SELECT 'PM-KISAN', 'occupation', 'EQUALS', 'farmer', true,
  'This scheme is only for farmers. Your occupation does not match.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-KISAN' AND r.field_name = 'occupation' AND r.operator = 'EQUALS'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'PM-KISAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 200000, true,
  'Your annual income exceeds Rs 2 lakh limit for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-KISAN' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-KISAN', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-KISAN' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-KISAN', 'Land Record / Khasra', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-KISAN' AND d.document_name = 'Land Record / Khasra');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-KISAN', 'Bank Passbook', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-KISAN' AND d.document_name = 'Bank Passbook');

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
SELECT 'PM-AWAS-RURAL', 'bpl_card', 'IS_TRUE', NULL, true,
  'This scheme requires a Below Poverty Line (BPL) card.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-AWAS-RURAL' AND r.field_name = 'bpl_card' AND r.operator = 'IS_TRUE'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'PM-AWAS-RURAL', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual income must be below Rs 3 lakh for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-AWAS-RURAL' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-AWAS-RURAL', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-AWAS-RURAL' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-AWAS-RURAL', 'BPL Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-AWAS-RURAL' AND d.document_name = 'BPL Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-AWAS-RURAL', 'Bank Account Details', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-AWAS-RURAL' AND d.document_name = 'Bank Account Details');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-AWAS-RURAL', 'Domicile Certificate', false
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-AWAS-RURAL' AND d.document_name = 'Domicile Certificate');

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
SELECT 'JKEDI-LOAN', 'state', 'EQUALS', 'JK', true,
  'This scheme is exclusively for residents of Jammu and Kashmir.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKEDI-LOAN' AND r.field_name = 'state' AND r.operator = 'EQUALS'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'JKEDI-LOAN', 'age', 'GREATER_THAN_OR_EQUAL', 18, true,
  'Minimum age requirement is 18 years for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKEDI-LOAN' AND r.field_name = 'age' AND r.operator = 'GREATER_THAN_OR_EQUAL'
      AND r.value_number = 18
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'JKEDI-LOAN', 'age', 'LESS_THAN_OR_EQUAL', 45, true,
  'Maximum age limit is 45 years for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKEDI-LOAN' AND r.field_name = 'age' AND r.operator = 'LESS_THAN_OR_EQUAL'
      AND r.value_number = 45
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'JKEDI-LOAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 500000, true,
  'Annual income must be below Rs 5 lakh for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKEDI-LOAN' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKEDI-LOAN', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKEDI-LOAN' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKEDI-LOAN', 'Domicile Certificate of J&K', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKEDI-LOAN' AND d.document_name = 'Domicile Certificate of J&K');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKEDI-LOAN', 'Business Plan', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKEDI-LOAN' AND d.document_name = 'Business Plan');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKEDI-LOAN', 'Bank Statement (6 months)', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKEDI-LOAN' AND d.document_name = 'Bank Statement (6 months)');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKEDI-LOAN', 'Income Certificate', false
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKEDI-LOAN' AND d.document_name = 'Income Certificate');

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
SELECT 'NSFDC-LOAN', 'caste_category', 'EQUALS', 'SC', true,
  'This scheme is only for Scheduled Caste individuals. Caste certificate required.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'NSFDC-LOAN' AND r.field_name = 'caste_category' AND r.operator = 'EQUALS'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'NSFDC-LOAN', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual family income must be below Rs 3 lakh for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'NSFDC-LOAN' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSFDC-LOAN', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSFDC-LOAN' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSFDC-LOAN', 'Caste Certificate (SC)', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSFDC-LOAN' AND d.document_name = 'Caste Certificate (SC)');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSFDC-LOAN', 'Income Certificate', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSFDC-LOAN' AND d.document_name = 'Income Certificate');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSFDC-LOAN', 'Bank Passbook', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSFDC-LOAN' AND d.document_name = 'Bank Passbook');

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
SELECT 'PM-SVANidhi', 'occupation', 'IN', 'street_vendor,vendor,hawker', true,
  'This scheme is only for street vendors and hawkers.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-SVANidhi' AND r.field_name = 'occupation' AND r.operator = 'IN'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-SVANidhi', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-SVANidhi' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-SVANidhi', 'Vendor Certificate or ULB Letter', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-SVANidhi' AND d.document_name = 'Vendor Certificate or ULB Letter');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-SVANidhi', 'Bank Account Details', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-SVANidhi' AND d.document_name = 'Bank Account Details');

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
SELECT 'MUDRA-SHISHU', 'income_annual', 'LESS_THAN_OR_EQUAL', 500000, true,
  'Annual income should be below Rs 5 lakh for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'MUDRA-SHISHU' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'MUDRA-SHISHU', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'MUDRA-SHISHU' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'MUDRA-SHISHU', 'Business Proof or Plan', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'MUDRA-SHISHU' AND d.document_name = 'Business Proof or Plan');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'MUDRA-SHISHU', 'Bank Statement (3 months)', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'MUDRA-SHISHU' AND d.document_name = 'Bank Statement (3 months)');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'MUDRA-SHISHU', 'Passport Size Photo', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'MUDRA-SHISHU' AND d.document_name = 'Passport Size Photo');

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
SELECT 'NSP-SCHOLARSHIP', 'is_student', 'IS_TRUE', NULL, true,
  'This scholarship is only for currently enrolled students.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'NSP-SCHOLARSHIP' AND r.field_name = 'is_student' AND r.operator = 'IS_TRUE'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'NSP-SCHOLARSHIP', 'income_annual', 'LESS_THAN_OR_EQUAL', 250000, true,
  'Family annual income must be below Rs 2.5 lakh for this scholarship.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'NSP-SCHOLARSHIP' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_string, is_mandatory, failure_message)
SELECT 'NSP-SCHOLARSHIP', 'caste_category', 'IN', 'SC,ST,OBC', true,
  'This scholarship is for SC, ST, and OBC category students only.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'NSP-SCHOLARSHIP' AND r.field_name = 'caste_category' AND r.operator = 'IN'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'Income Certificate', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'Income Certificate');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'Caste Certificate', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'Caste Certificate');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'Previous Year Marksheet', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'Previous Year Marksheet');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'College / Institution ID Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'College / Institution ID Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'NSP-SCHOLARSHIP', 'Bank Passbook', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'NSP-SCHOLARSHIP' AND d.document_name = 'Bank Passbook');

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
SELECT 'JKPMC-MUMKIN', 'state', 'EQUALS', 'JK', true,
  'This scheme is exclusively for residents of Jammu and Kashmir.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKPMC-MUMKIN' AND r.field_name = 'state' AND r.operator = 'EQUALS'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'JKPMC-MUMKIN', 'age', 'GREATER_THAN_OR_EQUAL', 18, true,
  'Minimum age is 18 years for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKPMC-MUMKIN' AND r.field_name = 'age' AND r.operator = 'GREATER_THAN_OR_EQUAL'
      AND r.value_number = 18
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'JKPMC-MUMKIN', 'age', 'LESS_THAN_OR_EQUAL', 35, true,
  'Maximum age limit is 35 years for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'JKPMC-MUMKIN' AND r.field_name = 'age' AND r.operator = 'LESS_THAN_OR_EQUAL'
      AND r.value_number = 35
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKPMC-MUMKIN', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKPMC-MUMKIN' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKPMC-MUMKIN', 'Domicile Certificate of J&K', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKPMC-MUMKIN' AND d.document_name = 'Domicile Certificate of J&K');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKPMC-MUMKIN', 'Qualification Certificate', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKPMC-MUMKIN' AND d.document_name = 'Qualification Certificate');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'JKPMC-MUMKIN', 'Bank Account Details', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'JKPMC-MUMKIN' AND d.document_name = 'Bank Account Details');

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
SELECT 'PM-UJJWALA', 'gender', 'EQUALS', 'female', true,
  'This scheme is only for women beneficiaries.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-UJJWALA' AND r.field_name = 'gender' AND r.operator = 'EQUALS'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_boolean, is_mandatory, failure_message)
SELECT 'PM-UJJWALA', 'bpl_card', 'IS_TRUE', NULL, true,
  'A BPL (Below Poverty Line) card is required for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'PM-UJJWALA' AND r.field_name = 'bpl_card' AND r.operator = 'IS_TRUE'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-UJJWALA', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-UJJWALA' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-UJJWALA', 'BPL Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-UJJWALA' AND d.document_name = 'BPL Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-UJJWALA', 'Bank Account Details', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-UJJWALA' AND d.document_name = 'Bank Account Details');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'PM-UJJWALA', 'Passport Size Photo', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'PM-UJJWALA' AND d.document_name = 'Passport Size Photo');

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
SELECT 'KISAN-CREDIT-CARD', 'is_farmer', 'IS_TRUE', NULL, true,
  'Kisan Credit Card is only available for farmers and cultivators.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'KISAN-CREDIT-CARD' AND r.field_name = 'is_farmer' AND r.operator = 'IS_TRUE'
);

INSERT INTO eligibility_rules (scheme_id, field_name, operator, value_number, is_mandatory, failure_message)
SELECT 'KISAN-CREDIT-CARD', 'income_annual', 'LESS_THAN_OR_EQUAL', 300000, true,
  'Annual income should be below Rs 3 lakh for this scheme.'
WHERE NOT EXISTS (
  SELECT 1 FROM eligibility_rules r
  WHERE r.scheme_id = 'KISAN-CREDIT-CARD' AND r.field_name = 'income_annual' AND r.operator = 'LESS_THAN_OR_EQUAL'
);

INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'KISAN-CREDIT-CARD', 'Aadhaar Card', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'KISAN-CREDIT-CARD' AND d.document_name = 'Aadhaar Card');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'KISAN-CREDIT-CARD', 'Land Record / Khasra', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'KISAN-CREDIT-CARD' AND d.document_name = 'Land Record / Khasra');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'KISAN-CREDIT-CARD', 'Passport Size Photo', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'KISAN-CREDIT-CARD' AND d.document_name = 'Passport Size Photo');
INSERT INTO scheme_documents (scheme_id, document_name, is_mandatory)
SELECT 'KISAN-CREDIT-CARD', 'Bank Account Details', true
WHERE NOT EXISTS (SELECT 1 FROM scheme_documents d WHERE d.scheme_id = 'KISAN-CREDIT-CARD' AND d.document_name = 'Bank Account Details');
