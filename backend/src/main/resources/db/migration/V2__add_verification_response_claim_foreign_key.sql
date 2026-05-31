ALTER TABLE verification_responses
    ADD CONSTRAINT fk_verification_responses_claim
    FOREIGN KEY (claim_id) REFERENCES match_requests(id);
