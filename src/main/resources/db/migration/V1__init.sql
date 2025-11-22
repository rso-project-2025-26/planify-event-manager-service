-- Events table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    
    -- Location reference
    location_id BIGINT,
    location_name VARCHAR(500),
    
    organizer_id BIGINT NOT NULL,
    max_attendees INTEGER,
    current_attendees INTEGER DEFAULT 0,
    
    -- Event visibility
    event_type VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_type ON events(event_type);
CREATE INDEX idx_events_location ON events(location_id);

COMMENT ON TABLE events IS 'Events managed by event-manager-service';
COMMENT ON COLUMN events.location_id IS 'References locations table in booking-service';
COMMENT ON COLUMN events.organizer_id IS 'References users table in user-service';
COMMENT ON COLUMN events.event_type IS 'PUBLIC events visible to all, PRIVATE only to invited guests';

-- Guest list table
CREATE TABLE guest_list (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    rsvp_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    role VARCHAR(50) DEFAULT 'ATTENDEE',
    
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    
    checked_in BOOLEAN DEFAULT FALSE,
    checked_in_at TIMESTAMP,
    
    notes TEXT,
    
    CONSTRAINT guest_list_event_fk FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT guest_list_unique UNIQUE (event_id, user_id)
);

CREATE INDEX idx_guest_list_event ON guest_list(event_id);
CREATE INDEX idx_guest_list_user ON guest_list(user_id);
CREATE INDEX idx_guest_list_status ON guest_list(rsvp_status);

COMMENT ON TABLE guest_list IS 'Guest list and RSVP tracking for events';
COMMENT ON COLUMN guest_list.rsvp_status IS 'Guest response to invitation';