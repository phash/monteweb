-- Billing periods for annual hour settlements
CREATE TABLE billing_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID REFERENCES users(id),
    report_data JSONB,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_billing_period_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_billing_period_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

-- Only one active period allowed at a time
CREATE UNIQUE INDEX uq_billing_periods_active
    ON billing_periods (status) WHERE status = 'ACTIVE';

CREATE INDEX idx_billing_periods_status ON billing_periods (status);
CREATE INDEX idx_billing_periods_dates ON billing_periods (start_date, end_date);
