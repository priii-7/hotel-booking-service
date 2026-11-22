CREATE TABLE IF NOT EXISTS hotels (
  id BIGINT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  city VARCHAR(100) NOT NULL,
  price_per_night NUMERIC(10,2) NOT NULL,
  rating NUMERIC(2,1) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_hotels_city_rating ON hotels (lower(city), rating DESC);

CREATE TABLE IF NOT EXISTS room_inventory (
  hotel_id BIGINT NOT NULL REFERENCES hotels(id),
  stay_date DATE NOT NULL,
  total_rooms INT NOT NULL,
  booked_rooms INT NOT NULL DEFAULT 0,
  PRIMARY KEY (hotel_id, stay_date),
  CHECK (booked_rooms >= 0 AND booked_rooms <= total_rooms)
);

CREATE TABLE IF NOT EXISTS bookings (
  id UUID PRIMARY KEY,
  hotel_id BIGINT NOT NULL REFERENCES hotels(id),
  guest_email VARCHAR(200) NOT NULL,
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
