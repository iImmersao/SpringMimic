package com.iimmersao.springmimic.model;

public class UserMapper {
    public static User toDTO(Object userEntity) {
        if (userEntity instanceof MySqlUser u) {
            return new User(String.valueOf(u.getId()), u.getUsername(), u.getEmail());
        } else if (userEntity instanceof H2User u) {
            return new User(String.valueOf(u.getId()), u.getUsername(), u.getEmail());
        } else if (userEntity instanceof MongoUser u) {
            return new User(u.getId(), u.getUsername(), u.getEmail());
        }
        throw new IllegalArgumentException("Unsupported entity type");
    }

    public static Object toEntity(User dto, String dbType) {
        if ("mysql".equalsIgnoreCase(dbType) || "h2".equalsIgnoreCase(dbType)) {
            MySqlUser user = new MySqlUser();
            if (dto.getId() != null) user.setId(Integer.parseInt(dto.getId()));
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        } else if ("mongodb".equalsIgnoreCase(dbType)) {
            MongoUser user = new MongoUser();
            user.setId(dto.getId());
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        }
        throw new IllegalArgumentException("Unsupported dbType");
    }
}
