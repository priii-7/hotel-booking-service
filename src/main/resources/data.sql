INSERT INTO hotels (id, name, city, price_per_night, rating) VALUES
 (1, 'Grand Palace', 'Delhi', 9500.00, 4.6),
 (2, 'Budget Inn', 'Delhi', 2200.00, 3.8),
 (3, 'Lotus Suites', 'Delhi', 5200.00, 4.2),
 (4, 'Sea Breeze', 'Mumbai', 8800.00, 4.5),
 (5, 'City Stay', 'Mumbai', 3100.00, 3.9),
 (6, 'Garden Villa', 'Bangkok', 4200.00, 4.4),
 (7, 'Riverside Hotel', 'Bangkok', 6100.00, 4.7)
ON CONFLICT (id) DO NOTHING;

INSERT INTO room_inventory (hotel_id, stay_date, total_rooms, booked_rooms)
SELECT h.id, d::date, 10, 0
FROM hotels h CROSS JOIN generate_series(current_date, current_date + 180, interval '1 day') AS d
ON CONFLICT DO NOTHING;
