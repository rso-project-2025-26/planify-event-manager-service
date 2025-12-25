-- Add booking link fields to events
ALTER TABLE events ADD COLUMN IF NOT EXISTS booking_id UUID;
ALTER TABLE events ADD COLUMN IF NOT EXISTS booking_status VARCHAR(50);

COMMENT ON COLUMN events.booking_id IS 'ID of booking in booking-service';
COMMENT ON COLUMN events.booking_status IS 'Cached booking status from booking-service';
