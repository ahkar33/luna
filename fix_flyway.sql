-- Delete the V6 migration entry from Flyway schema history
DELETE FROM flyway_schema_history WHERE version = '6';
