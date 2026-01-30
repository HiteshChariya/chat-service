-- Chat service uses schema conf (shared with other services).
-- Tables are created automatically by JPA with ddl-auto: update.
-- Reference schema for documentation:

-- CREATE SCHEMA IF NOT EXISTS conf;

-- conf.chat_room: one support thread per user
-- CREATE TABLE conf.chat_room (
--   id BIGSERIAL PRIMARY KEY,
--   user_id BIGINT NOT NULL UNIQUE,
--   created_at TIMESTAMP NOT NULL,
--   updated_at TIMESTAMP NOT NULL
-- );

-- conf.chat_message: messages in a room
-- CREATE TABLE conf.chat_message (
--   id BIGSERIAL PRIMARY KEY,
--   chat_room_id BIGINT NOT NULL REFERENCES conf.chat_room(id) ON DELETE CASCADE,
--   sender_id BIGINT NOT NULL,
--   sender_role VARCHAR(20) NOT NULL,
--   sender_display_name VARCHAR(255),
--   content TEXT NOT NULL,
--   created_at TIMESTAMP NOT NULL
-- );

-- CREATE INDEX idx_chat_message_room ON conf.chat_message(chat_room_id);
-- CREATE INDEX idx_chat_message_created ON conf.chat_message(created_at);

-- conf.chat_room_read: when each user last read each room (for unread counts)
-- CREATE TABLE conf.chat_room_read (
--   id BIGSERIAL PRIMARY KEY,
--   chat_room_id BIGINT NOT NULL REFERENCES conf.chat_room(id) ON DELETE CASCADE,
--   user_id BIGINT NOT NULL,
--   last_read_at timestamp without time zone NOT NULL DEFAULT now(),
--   UNIQUE(chat_room_id, user_id)
-- );
